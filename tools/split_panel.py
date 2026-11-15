"""
按「几何 + UV」双重判定把车厢板 OBJ 拆成 body / glass 两层。

为什么不用纯 UV 判定：
    窗框内侧壁和玻璃共用同一片 UV 矩形，纯 UV 会把窗框也算进玻璃层，
    导致 27 个面里只有 2 个是真玻璃，其余是框 —— 游戏里表现为窗户缺一半。

判定规则（同时满足才算玻璃）：
    1. 面的顶点全部落在窗洞的 XY 范围内（由 --window-x/y 给出）
    2. 面法线沿 ±Z（板面法线方向）
    3. 该面的 UV 落在窗户贴图区域

用法：
    python split_panel.py <源.obj> <输出目录> <目标名前缀>
        --window-x 0.25 1.75 --window-y 0.89 1.50
        --uv-u 0 0.4531 --uv-v 0.3125 0.5664
"""
import argparse
import os
import sys


def parse_obj(path):
    vs, vt, vn, faces = [], [], [], []
    for line in open(path, encoding='utf-8', errors='replace'):
        s = line.split()
        if not s:
            continue
        if s[0] == 'v':
            vs.append(tuple(float(x) for x in s[1:4]))
        elif s[0] == 'vt':
            vt.append((float(s[1]), float(s[2])))
        elif s[0] == 'vn':
            vn.append(tuple(float(x) for x in s[1:4]))
        elif s[0] == 'f':
            faces.append(s[1:])
    return vs, vt, vn, faces


def face_indices(face):
    """返回 (顶点索引列表, UV索引列表, 法线索引列表)，均为 0-based。"""
    vi, ti, ni = [], [], []
    for p in face:
        seg = p.split('/')
        vi.append(int(seg[0]) - 1)
        if len(seg) >= 2 and seg[1]:
            ti.append(int(seg[1]) - 1)
        if len(seg) >= 3 and seg[2]:
            ni.append(int(seg[2]) - 1)
    return vi, ti, ni


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('source')
    ap.add_argument('outdir')
    ap.add_argument('target')
    ap.add_argument('--window-x', nargs=2, type=float, default=None)
    ap.add_argument('--window-y', nargs=2, type=float, default=None)
    ap.add_argument('--uv-u', nargs=2, type=float, default=None)
    ap.add_argument('--uv-v', nargs=2, type=float, default=None)
    args = ap.parse_args()

    vs, vt, vn, faces = parse_obj(args.source)
    print(f'顶点 {len(vs)}  UV {len(vt)}  法线 {len(vn)}  面 {len(faces)}')

    glass, body = [], []
    for face in faces:
        vi, ti, ni = face_indices(face)
        pts = [vs[i] for i in vi]

        is_glass = True

        # 1. 几何：XY 落在窗洞内
        if args.window_x:
            xs = [p[0] for p in pts]
            if not (min(xs) >= args.window_x[0] - 1e-4 and max(xs) <= args.window_x[1] + 1e-4):
                is_glass = False
        if args.window_y and is_glass:
            ys = [p[1] for p in pts]
            if not (min(ys) >= args.window_y[0] - 1e-4 and max(ys) <= args.window_y[1] + 1e-4):
                is_glass = False

        # 2. 法线沿 ±Z（板面法线方向）—— 排除窗框侧壁
        if is_glass and ni:
            nz = sum(vn[i][2] for i in ni) / len(ni)
            if abs(nz) < 0.9:
                is_glass = False

        # 3. UV 落在窗户贴图区域
        #
        #    默认【不做】这个判定，只用几何（--window-x/--window-y + 法线）。
        #    原因：同一个窗洞的内表面和外表面，UV 可能完全不同
        #    （实测宽板内表面 UV 是 0.266..0.453，外表面是 0..0.188）。
        #    如果同时按 UV 卡，内表面会被判成主体，从而把窗洞堵死 ——
        #    表现就是「从里面看不见外面」。
        #    只有显式传 --uv-u/--uv-v 时才启用这个额外过滤。
        if is_glass and args.uv_u and args.uv_v and ti:
            for i in ti:
                u, v = vt[i]
                if not (args.uv_u[0] - 1e-4 <= u <= args.uv_u[1] + 1e-4 and
                        args.uv_v[0] - 1e-4 <= v <= args.uv_v[1] + 1e-4):
                    is_glass = False
                    break

        (glass if is_glass else body).append(face)

    print(f'主体面 {len(body)} / 窗户面 {len(glass)}')

    if not glass:
        print('⚠️ 没有面被判定为玻璃，检查 --window-*/--uv-* 参数', file=sys.stderr)

    os.makedirs(args.outdir, exist_ok=True)

    # 保留原文件的 v/vt/vn 顶点段
    v_lines, vt_lines, vn_lines = [], [], []
    for line in open(args.source, encoding='utf-8', errors='replace'):
        s = line.split()
        if not s:
            continue
        if s[0] == 'v':
            v_lines.append(line.rstrip('\r\n'))
        elif s[0] == 'vt':
            vt_lines.append(line.rstrip('\r\n'))
        elif s[0] == 'vn':
            vn_lines.append(line.rstrip('\r\n'))

    def write_part(suffix, face_list, *, vn_override=None, extra_vn=None):
        """
        写一个 OBJ 分片。

        vn_override: 可选的 {原法线索引: 新法线索引}，用于复制的反向面
        extra_vn:    需要追加到文件尾部的法线行列表（与 vn_override 配合）
        """
        path = os.path.join(args.outdir, f'{args.target}_{suffix}.obj')
        with open(path, 'w', encoding='utf-8', newline='\n') as f:
            f.write(f'# 400AF {args.target} - {suffix}\n')
            f.write('# 由 tools/split_panel.py 生成，勿手改\n')
            f.write(f'mtllib {args.target}.mtl\n\n')
            f.write(f'o {args.target}_{suffix}\n')
            for l in v_lines:
                f.write(l + '\n')
            for l in vt_lines:
                f.write(l + '\n')
            for l in vn_lines:
                f.write(l + '\n')
            if extra_vn:
                for l in extra_vn:
                    f.write(l + '\n')
            f.write('\nusemtl t0\n')
            for face in face_list:
                f.write('f ' + ' '.join(face) + '\n')
        print(f'  {os.path.basename(path)}  {len(face_list)} 个面')

    # 玻璃层要双面可见：原面（朝外）+ 复制一份反绕序的面（朝内）。
    #
    # 为什么必须补：
    #   OBJ 导出的玻璃往往只有一层朝外的三角形（法线 +Z）。MC 会剔除背面，
    #   于是「从外面能看穿、从里面看是黑墙」。补一层反绕序的面就能双面可见。
    glass_double = []
    flip_vn = {}                       # 原 vn 索引 -> 反向 vn 索引
    extra_vn_lines = []

    for face in glass:
        # 原面
        glass_double.append(face)
        # 反向面：顶点顺序反过来，法线换成取反的新法线
        rev = []
        for p in reversed(face):
            seg = p.split('/')
            new_p = p
            if len(seg) >= 3 and seg[2]:
                ni = int(seg[2])
                if ni not in flip_vn:
                    n = vn[ni - 1]
                    extra_vn_lines.append(f'vn {-n[0]:.6f} {-n[1]:.6f} {-n[2]:.6f}')
                    flip_vn[ni] = len(vn) + len(extra_vn_lines)   # 1-based 新索引
                new_p = f'{seg[0]}/{seg[1]}/{flip_vn[ni]}' if len(seg) >= 2 and seg[1] else f'{seg[0]}//{flip_vn[ni]}'
            rev.append(new_p)
        glass_double.append(rev)

    write_part('body', body)
    write_part('glass', glass_double, extra_vn=extra_vn_lines)
    if extra_vn_lines:
        print(f'  玻璃层补了 {len(extra_vn_lines)} 条反向法线 -> 双面可见')


if __name__ == '__main__':
    main()
