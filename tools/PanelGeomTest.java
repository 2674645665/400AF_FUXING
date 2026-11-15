import java.util.*;

/**
 * 独立验证 CarriagePanelBlock 的几何逻辑：碰撞盒 + 占位格位置。
 * 不依赖 Minecraft，纯数学复现。
 */
public class PanelGeomTest {

    static final int WIDTH = 1, HEIGHT = 2;
    static final double DEPTH_MIN = 0.6 / 16.0, DEPTH_MAX = 3.0 / 16.0;

    static int[] offsetOf(int part) {
        if (part <= 0) return new int[]{0, 0, 0};
        int col = part % WIDTH;
        int row = part / WIDTH;
        return new int[]{col, row, 0};
    }

    enum Dir { NORTH, EAST, SOUTH, WEST;
        Dir clockWise() { return values()[(ordinal() + 1) % 4]; }
    }

    static int[] worldPosOf(int[] root, int part, Dir facing) {
        int[] off = offsetOf(part);
        Dir right = facing.clockWise();
        int dx = 0, dz = 0;
        switch (right) {
            case NORTH -> dz = -1;
            case SOUTH -> dz = 1;
            case EAST -> dx = 1;
            case WEST -> dx = -1;
        }
        return new int[]{ root[0] + dx * off[0], root[1] + off[1], root[2] + dz * off[0] };
    }

    // 修复后的 rootOf
    static int[] rootOfFixed(int[] pos, int part, Dir facing) {
        if (part <= 0) return pos;
        int col = part % WIDTH;
        int row = part / WIDTH;
        Dir right = facing.clockWise();
        int dx = 0, dz = 0;
        switch (right) {
            case NORTH -> dz = -1;
            case SOUTH -> dz = 1;
            case EAST -> dx = 1;
            case WEST -> dx = -1;
        }
        return new int[]{ pos[0] - dx * col, pos[1] - row, pos[2] - dz * col };
    }

    // 旧的错误 rootOf
    static int[] rootOfOld(int[] pos, int part, Dir facing) {
        int[] off = offsetOf(part);
        return new int[]{ pos[0] - off[0], pos[1] - off[1], pos[2] };
    }

    // 碰撞盒：NORTH 基准 x 0..16, y 0..16, z zmin..zmax，再按 facing 旋转 90°*q
    static double[] shape(Dir facing) {
        double x1 = 0, z1 = DEPTH_MIN * 16, x2 = 16, z2 = DEPTH_MAX * 16;
        int q = switch (facing) { case NORTH -> 0; case EAST -> 1; case SOUTH -> 2; case WEST -> 3; };
        for (int i = 0; i < q; i++) {
            double nx1 = 16.0 - z2, nz1 = x1;
            double nx2 = 16.0 - z1, nz2 = x2;
            x1 = nx1; z1 = nz1; x2 = nx2; z2 = nz2;
        }
        return new double[]{x1, z1, x2, z2};
    }

    public static void main(String[] args) {
        System.out.println("=== 1. 碰撞盒是否在 0..16 范围内 ===");
        boolean okShape = true;
        for (Dir d : Dir.values()) {
            double[] s = shape(d);
            boolean in = s[0] >= 0 && s[2] <= 16 && s[1] >= 0 && s[3] <= 16 && s[0] < s[2] && s[1] < s[3];
            System.out.printf("  %-6s x %.2f..%.2f  z %.2f..%.2f  %s%n", d, s[0], s[2], s[1], s[3], in ? "OK" : "越界!");
            okShape &= in;
        }
        System.out.println("  => " + (okShape ? "全部合法" : "有越界"));

        System.out.println("\n=== 2. rootOf 反推是否与 worldPosOf 互逆 ===");
        int[] root = {100, 64, 200};
        boolean okRoot = true;
        for (Dir d : Dir.values()) {
            for (int part = 0; part < WIDTH * HEIGHT; part++) {
                int[] world = worldPosOf(root, part, d);
                int[] backFixed = rootOfFixed(world, part, d);
                int[] backOld = rootOfOld(world, part, d);
                boolean fixedOk = Arrays.equals(backFixed, root);
                boolean oldOk = Arrays.equals(backOld, root);
                System.out.printf("  %-6s part=%d world=%s  修复后=%s %s   旧版=%s %s%n",
                        d, part, Arrays.toString(world),
                        Arrays.toString(backFixed), fixedOk ? "OK" : "错",
                        Arrays.toString(backOld), oldOk ? "OK" : "错(已修)");
                okRoot &= fixedOk;
            }
        }
        System.out.println("  => " + (okRoot ? "修复后全部互逆" : "仍有错误"));

        System.out.println("\n=== 3. 每个占位格都有对应碰撞盒 ===");
        for (Dir d : Dir.values()) {
            for (int part = 0; part < WIDTH * HEIGHT; part++) {
                double[] s = shape(d);
                boolean valid = s[2] > s[0] && s[3] > s[1];
                System.out.printf("  %-6s part=%d 碰撞盒存在=%s%n", d, part, valid);
            }
        }
    }
}
