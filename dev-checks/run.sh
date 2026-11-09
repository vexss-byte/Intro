#!/usr/bin/env bash
# Compiles every Minecraft-free class and runs the logic checks (needs only a JDK 21+; no Gradle, no Minecraft).
set -e
cd "$(dirname "$0")/.."
OUT=$(mktemp -d); STUB=$(mktemp -d)/dev/introcinema/common/config; mkdir -p "$STUB"
cp dev-checks/IntroConfigStub.java.txt "$STUB/IntroConfig.java"
C=src/client/java/dev/introcinema/client
javac -Xlint:all -d "$OUT" $(find $C/animation $C/camera $C/cinematic $C/fx -name '*.java' ! -name 'PlayerModelAnimator.java' ! -name 'CameraCollision.java' ! -name 'IntroDirector.java' ! -name 'CinematicSequence.java' ! -name 'PlayerPoseController.java') \
  $C/hud/TextAnimator.java $C/hud/HudState.java \
  src/main/java/dev/introcinema/common/roster/*.java src/main/java/dev/introcinema/common/session/SessionPlan.java \
  src/main/java/dev/introcinema/common/dimension/ArenaLayout.java \
  "$STUB/IntroConfig.java" dev-checks/Check.java dev-checks/Check2.java
java -cp "$OUT" Check
java -cp "$OUT" Check2
