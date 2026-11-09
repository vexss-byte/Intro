import dev.introcinema.client.fx.*;
import dev.introcinema.client.hud.TextAnimator;
import dev.introcinema.common.dimension.ArenaLayout;
import java.util.*;

public class Check2 {
  static int fails = 0;
  static void ok(boolean c, String m){ if(!c){fails++; System.out.println("FAIL: "+m);} else System.out.println("ok:   "+m); }

  public static void main(String[] a){
    // ---- arena layout ----
    for (int total : new int[]{1, 7, 40, 150}) {
      Set<String> seen = new HashSet<>(); boolean faces = true, onFloor = true, distinct = true;
      for (int i = 0; i < total; i++) {
        var s = ArenaLayout.audience(i, total);
        if (!seen.add(Math.round(s.x()*10)+","+Math.round(s.z()*10))) distinct = false;
        double dx = ArenaLayout.STAGE_X - s.x(), dz = ArenaLayout.STAGE_Z - s.z(), len = Math.hypot(dx, dz);
        double fx = -Math.sin(Math.toRadians(s.yaw())), fz = Math.cos(Math.toRadians(s.yaw()));
        if ((fx*dx + fz*dz)/len < 0.995) faces = false;
        if (Math.hypot(s.x(), s.z()) > ArenaLayout.ARENA_RADIUS - 4 || s.y() != ArenaLayout.FLOOR_Y + 1) onFloor = false;
      }
      ok(distinct, "audience slots unique for "+total+" players (no hard limit)");
      ok(faces, "audience faces the stage for "+total);
      ok(onFloor, "audience inside the arena floor for "+total);
    }
    Set<String> fs = new HashSet<>(); boolean fdist = true;
    for (int i = 0; i < 100; i++) { var s = ArenaLayout.finale(i, 100); if (!fs.add(Math.round(s.x()*10)+","+Math.round(s.z()*10))) fdist = false; }
    ok(fdist, "finale formation slots unique for 100 players");
    ok(ArenaLayout.finaleRows(100) == 10 && ArenaLayout.finaleRows(11) == 1 && ArenaLayout.finaleRows(12) == 2, "finale row count");

    // ---- projection ----
    FxProjection pr = new FxProjection(); float[] o = new float[4];
    pr.set(0, 0, 0, 0f, 0f, 70f, 1000, 500);
    ok(pr.project(0, 0, 10, o) && Math.abs(o[0]-500) < 0.5 && Math.abs(o[1]-250) < 0.5, "point straight ahead lands at screen centre");
    ok(!pr.project(0, 0, -5, o), "point behind the camera is rejected");
    ok(pr.project(-2, 0, 10, o) && o[0] > 500, "facing +Z, a point at -X (west) is on the right");
    ok(pr.project(0, 2, 10, o) && o[1] < 250, "higher point is higher on screen");
    pr.set(0, 0, 0, 90f, 0f, 70f, 1000, 500);
    ok(pr.project(-10, 0, 0, o) && Math.abs(o[0]-500) < 0.5, "yaw 90 looks toward -X");
    pr.set(0, 0, 0, 0f, 45f, 70f, 1000, 500);
    ok(pr.project(0, -10, 10, o) && Math.abs(o[1]-250) < 0.5, "pitch 45 looks down");

    // ---- particles ----
    ParticleController pc = new ParticleController(64);
    int accepted = 0; for (int i = 0; i < 500; i++) if (pc.emit(0, 0,0,0, 0,0,0, 5, 0.1f, 0xFFFFFF, 0, 1f)) accepted++;
    ok(accepted == pc.capacity() && pc.count() == pc.capacity(), "particle pool is capped ("+pc.capacity()+")");
    for (int i = 0; i < 6; i++) pc.update();
    ok(pc.count() == 0, "dead particles are all removed");
    ok(pc.rand() >= 0 && pc.rand() < 1, "rand in range");

    // ---- screen effects ----
    ScreenEffectController se = new ScreenEffectController();
    se.pixelTransition(100, 120); se.fade(200, 220, 0f, 1f); se.flash(50, 10, 1f, 0xFFFFFF);
    ok(se.transitionAmount(99) == 0 && se.transitionAmount(110) > 0.99 && se.transitionAmount(121) == 0, "transition peaks mid-window then clears");
    ok(se.fadeAlpha(199) == 0 && se.fadeAlpha(210) > 0.45 && se.fadeAlpha(500) == 1f, "fade holds at its end value");
    ok(se.flashAmount(50) > 0.99 && se.flashAmount(61) == 0, "flash decays to zero");
    se.reset(); ok(se.fadeAlpha(500) == 0 && se.transitionAmount(110) == 0, "reset leaves no overlay (no stuck fade)");

    // ---- fx manager lifecycle + pooling ----
    CinematicFXManager fx = new CinematicFXManager(); fx.configure(300);
    for (int i = 0; i < 200; i++) { fx.energyRing(0xFFC83C, 0,65,0, 5f, 10); fx.floatingGlow(0xFFFFFF, 0,65,0, 8f, 30, 40); fx.lightRays(0xFFFFFF, 0,65,0, 3, 20); fx.portal(0xFFFFFF,0,65,0,4f,20,i); }
    for (int i = 0; i < 40; i++) fx.update(i);
    ok(fx.particles().count() <= fx.particles().capacity(), "particles never exceed capacity under heavy emission");
    fx.cleanup();
    ok(fx.particles().count() == 0, "cleanup drops all particles");
    FxParams fp = new FxParams(); fx.fill(fp); ok(fp.impact == 0 && fp.portal == 0, "cleanup resets shader intensities");
    final int[] fills = {0};
    Painter p = new Painter(){ public int width(){return 1000;} public int height(){return 500;}
      public void fill(int a1,int b,int c,int d,int e){fills[0]++;} public void gradient(int a1,int b,int c,int d,int e,int f){fills[0]++;} };
    FxProjection pj = new FxProjection(); pj.set(0, 66, -8, 0f, 0f, 70f, 1000, 500);
    fx.burst(0xFFC83C, 0, 66, 0, 1.3f); fx.lightRays(0xFFFFFF, 0, 65, 0, 3, 20); fx.update(0); fx.render(p, pj);
    ok(fills[0] > 20, "render draws particles and beams ("+fills[0]+" draw calls)");

    // ---- text animator ----
    ok(TextAnimator.alpha(5,10,50,10)==0 && TextAnimator.alpha(30,10,50,10)==1f && Math.abs(TextAnimator.alpha(15,10,50,10)-0.5f)<1e-5, "alpha fades in/out");
    ok(TextAnimator.typewriter("Vexzz00",0.5f).equals("Vex"), "typewriter");
    ok(TextAnimator.lerpColor(0xFFD24A,0xFF8A1F,0f)==0xFFD24A && TextAnimator.lerpColor(0xFFD24A,0xFF8A1F,1f)==0xFF8A1F, "gradient endpoints");
    ok(TextAnimator.parseHex("#FFD24A",0)==0xFFD24A && TextAnimator.parseHex("bad",7)==7, "hex parse with fallback");
    System.out.println(fails==0 ? "ALL PASSED" : fails+" FAILED"); System.exit(fails==0?0:1);
  }
}
