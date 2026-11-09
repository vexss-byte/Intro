# Intro Member (Fabric, Minecraft 26.3)

Cinematic multiplayer introduction. `/intro start` moves every online player into the built-in dimension
`intro_member:intro_arena`, introduces them one by one as **real players** (no NPC, no clones) with a custom camera,
animation engine, HUD, particle/FX engine and GLSL layer, then restores everyone. No AzureLib, no Player Animation Library.

Toolchain (taken from FabricMC/fabric-example-mod, Fabric API 26.3 branch): Minecraft 26.3, Loader 0.20.1,
Loom 1.19-SNAPSHOT, Fabric API 0.165.2+26.3, Gradle 9.8.2, Java 25.

Build: `./gradlew build` -> `build/libs/intro-member-0.1.0.jar`. CI: `.github/workflows/build.yml`.

## Honest status

| Level | State |
|---|---|
| Source code written | all modules listed below |
| Pure-Java logic compiled + tested | **yes** — `bash dev-checks/run.sh` (JDK 21+, no Minecraft): 64 checks pass |
| GLSL syntax | **yes** — all 8 fragment shaders + the vertex shader pass `glslangValidator` (GLSL 330 core) |
| Minecraft-facing code compiled with Loom | **Partly**: in the first real CI run `compileJava` (all server/common code) passed; `compileClientJava` had 2 errors, now fixed (`GL14.glBlendFuncSeparate`, `mc.gui.screen()`). A new CI run is needed to confirm the client side and `build`. Whole-source `javac` shows zero syntax errors; imports were cross-checked against a mod that runs on 26.3 and the Fabric API 26.3 source (74/75 confirmed; the 75th is vanilla `EntityArgument`) |
| Game launched / feature tested in game | **NO** |
| Multiplayer / dedicated server tested | **NO** |

First step for you: push and run the GitHub Action, then send me the compiler log. Expect a few signature fixes.

## What is implemented

- **Server (`common/`)**: config with safe fallbacks; join-ordered roster frozen at start (UUID identity, dynamic size);
  silhouette members (`/intro silhouette add|remove|list`, must be online, name is NOT sent to clients until the reveal);
  per-player atomic JSON snapshots; session state machine (prepare -> teleport -> run -> restore); a single tick plan
  (`SessionPlan`) shared with clients; server heartbeat; slot enforcement; damage blocked for participants during the show
  (health is never edited); restore with verification + retries; disconnect/rejoin/restart recovery from snapshots;
  `/intro start|stop|status|skip|reload`; auto-start (off by default).
- **Dimension/arena**: data-driven dimension + dimension type (schema from the 26.3 data-pack docs); the arena is built
  once in code, a few chunks per tick, validated by real blocks before any teleport; never touches survival worlds.
- **Client engines**: keyframe animation (6 easings, clips, blender, per-player engine, pose applied on the render state
  via mixins and reset every frame), camera timeline (dolly/orbit/low angle/FOV, blended shots, collision, smoothing),
  one multi-track cinematic timeline with a catch-up scheduler, HUD (`[NN] Name` gold->orange banner, bottom-left card,
  quote line, title, caption), pooled particle/FX engine drawn in the GUI layer, screen effects (fade, flash, pixel
  transition, vignette) and an optional GLSL layer (nebula, energy, portal, glow, impact, streaks, bloom, vignette/grade).
- **Reference-video behaviour**: silhouette `[NN] ???` then reveal; last member gets caption "DAN YANG TERAKHIR..." and
  card text "THE LAST MEMBER"; per-member particle colour; pixel-dissolve between members; wide finale + title.

## Known gaps / unverified points (not hidden)

- **Sound**: the sound track exists on the timeline but no sounds are shipped.
- **Particles are screen-space** (projected, drawn in the HUD pass): no depth occlusion by blocks/players.
- **Silhouette look** (black skin, hidden items) uses reflection on `PlayerSkin`/`ClientAsset`; if those shapes differ
  it falls back to "name hidden only" and logs once. Armor can still show on silhouettes.
- **GLSL layer** reads/draws on the framebuffer bound at `LevelRenderEvents.END_MAIN` with raw OpenGL. It is additive /
  multiplicative only, restores all GL state, and turns itself off after repeated GL errors, but it is untested on a GPU.
- **Optional mixins** (`HumanoidModelMixin` armor pose, `CameraRollMixin` roll) live in a non-required config; if their
  targets differ they are skipped with a warning (armor then ignores poses, roll is ignored).
- Input lock = `KeyMapping.releaseAll()` each tick + mouse-turn redirect + server slot enforcement. A key pressed between
  ticks can still nudge the player for one tick; the server pulls them back.
- `PlayerPoseController` exists; `IntroAnimationEngine` also covers the spec's pose-controller role.
- Spec names `IntroClientController` under `common/session`; it lives in the client source set because it uses client classes.
- Not done: auto-start "server startup condition" beyond the once-per-start flag; stress test with many players.

## Config (`config/intro_member.json`)
Written on first start. Invalid values are clamped; a broken file falls back to defaults and is left untouched.
Notable keys: `serverTitle`, `subtitle`, `lastMemberLabel`, `lastCaption`, `silhouetteMembers` (UUIDs),
`memberQuotes` (UUID -> text), durations in ticks, `particleQuality` LOW|MEDIUM|HIGH, `bloomEnabled`,
`shaderEnabled`, `nameplateColor`/`nameplateGradientEnd`/`nameplatePosition`/`nameplateScale`.
Visual keys are read by each client from its own config file.

## Layout
`dev.introcinema`: `IntroCinemaMod`, `common/{config,roster,session,network,teleport,dimension,command}`,
`client/{animation,camera,cinematic,hud,fx,shader,render,session}`, `mixin/client`.
Assets: `assets/intro_member/{shaders,textures,lang}`, data: `data/intro_member/{dimension,dimension_type}`.
