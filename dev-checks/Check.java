import dev.introcinema.client.animation.*;
import dev.introcinema.client.animation.PlayerPose.Part;
import dev.introcinema.client.camera.*;
import dev.introcinema.client.cinematic.*;
import dev.introcinema.common.config.IntroConfig;
import dev.introcinema.common.roster.*;
import dev.introcinema.common.session.SessionPlan;
import java.util.*;

public class Check {
  static int fails = 0;
  static void ok(boolean c, String m){ if(!c){fails++; System.out.println("FAIL: "+m);} else System.out.println("ok:   "+m); }
  static boolean near(float a,float b){ return Math.abs(a-b)<1e-3f; }
  public static void main(String[] a){
    // easing endpoints
    for (var e : AnimationInterpolator.Easing.values()) ok(near(e.apply(0),0)&&near(e.apply(1),1), "easing "+e+" hits 0 and 1");
    ok(near(AnimationInterpolator.lerpAngle(350,10,0.5f),360f)||near(AnimationInterpolator.lerpAngle(350,10,0.5f),0f), "lerpAngle takes shortest arc");
    // clip sampling
    PlayerPose p = new PlayerPose();
    PoseLibrary.get(PoseLibrary.HERO).sample(50, p);
    ok(near(p.rot(Part.RIGHT_ARM,0), -160), "hero pose ends with right arm at -160");
    ok(near(p.v[PlayerPose.index(Part.HEAD,PlayerPose.SCALE)],1f), "neutral scale stays 1");
    // blender crossfade
    AnimationBlender b = new AnimationBlender(); PlayerPose shown = new PlayerPose();
    b.transitionTo(PoseLibrary.get(PoseLibrary.HERO), 0, 10, shown);
    PlayerPose o = new PlayerPose(); b.sample(0, o);
    ok(near(o.rot(Part.RIGHT_ARM,0),0), "blend starts from previous pose");
    b.sample(100, o); ok(o.rot(Part.RIGHT_ARM,0) < -100, "blend ends on clip pose");
    // engine reset
    IntroAnimationEngine eng = new IntroAnimationEngine(); UUID id = UUID.randomUUID();
    eng.play(id, PoseLibrary.VICTORY, 0, 5);
    ok(eng.poseFor(id, 60) != null, "engine returns pose while active");
    eng.stopAll(); ok(eng.poseFor(id, 61) == null, "stopAll leaves no pose override");
    // camera
    CameraPose lp = CameraPose.lookAt(0,0,-10, 0,0,0, 0, 70);
    ok(near(lp.yaw(),0) && near(lp.pitch(),0), "lookAt +Z gives yaw 0 pitch 0");
    CameraTimeline ct = new CameraTimeline(10)
      .add(0,100, CameraTimeline.move(new CameraPose(0,0,0,0,0,0,70), new CameraPose(10,0,0,0,0,0,70)), AnimationInterpolator.Easing.LINEAR)
      .add(100,200, CameraTimeline.still(new CameraPose(50,0,0,0,0,0,50)), AnimationInterpolator.Easing.LINEAR);
    CameraPose s0 = ct.sample(100), s1 = ct.sample(105), s2 = ct.sample(120);
    ok(Math.abs(s0.x()-10)<1e-6, "no jump at shot boundary (x=10 at tick 100)");
    ok(s1.x()>10 && s1.x()<50, "camera is blending between shots at tick 105");
    ok(Math.abs(s2.x()-50)<1e-6, "next shot reached after blend");
    // scheduler: late catch-up fires everything exactly once, in order
    CinematicTimeline tl = new CinematicTimeline(100); List<String> log = new ArrayList<>();
    tl.track("a").at(10,"a10",()->log.add("a10")).at(50,"a50",()->log.add("a50"));
    tl.track("b").at(30,"b30",()->log.add("b30")).at(60,"bad",()->{throw new RuntimeException("x");}).at(70,"b70",()->log.add("b70"));
    List<String> errs = new ArrayList<>();
    CinematicScheduler sc = new CinematicScheduler(tl,(e,t)->errs.add(e.name()));
    sc.advanceTo(20); sc.advanceTo(20); sc.advanceTo(5);
    ok(log.equals(List.of("a10")), "repeated/older ticks don't refire");
    sc.advanceTo(90);
    ok(log.containsAll(List.of("a10","a50","b30","b70")) && log.size()==4, "late jump fires missed events once");
    ok(errs.equals(List.of("bad")), "failing event isolated, others still run");
    ok(!sc.finished() && sc.advanceTo(100), "finishes at duration");
    // session plan
    IntroConfig c = new IntroConfig();
    SessionPlan sp = SessionPlan.of(c, 40);
    ok(sp.total()==200+40*140+260+40, "plan total ticks");
    ok(sp.sceneAt(0)==SessionPlan.Scene.OPENING && sp.sceneAt(200)==SessionPlan.Scene.MEMBER_REVEAL
       && sp.sceneAt(sp.finaleStart())==SessionPlan.Scene.FINALE && sp.sceneAt(sp.total())==SessionPlan.Scene.DONE, "scene boundaries");
    ok(sp.memberIndexAt(200)==0 && sp.memberIndexAt(200+140)==1 && sp.memberIndexAt(sp.finaleStart()-1)==39, "member index: nobody skipped");
    // roster
    RosterManager rm = new RosterManager(); UUID[] u = new UUID[5];
    for (int i=0;i<5;i++){ u[i]=UUID.randomUUID(); rm.onJoin(u[i],"P"+(i+1)); }
    rm.onJoin(u[0],"Renamed");
    c.silhouetteMembers.add(u[1].toString()); c.silhouetteMembers.add(UUID.randomUUID().toString()); c.silhouetteMembers.add("nope");
    var fr = rm.freeze(c);
    ok(fr.entries().size()==5 && fr.entries().get(0).name().equals("P1"), "join order kept, UUID is identity");
    ok(fr.entries().get(1).silhouette() && !fr.entries().get(2).silhouette(), "only chosen online player is silhouette");
    ok(fr.skippedSilhouettes().size()==2, "offline/invalid silhouettes reported, not fatal");
    ok(fr.entries().get(4).last() && !fr.entries().get(3).last(), "last member flagged");
    rm.onDisconnect(u[2]); ok(fr.entries().size()==5 && rm.onlineCount()==4, "frozen roster unaffected by disconnect");
    System.out.println(fails==0 ? "ALL PASSED" : fails+" FAILED"); System.exit(fails==0?0:1);
  }
}
