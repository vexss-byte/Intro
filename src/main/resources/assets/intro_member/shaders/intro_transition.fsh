// Motion streaks toward the focus during member-to-member transitions (additive, never blurs the scene).
void main() {
    vec2 d = fromCenter(vUv);
    float r = length(d), a = atan(d.y, d.x);
    float streak = pow(vnoise(vec2(a * 40.0, floor(uTime * 24.0))), 4.0);
    float m = smoothstep(0.05, 0.7, r) * streak;
    vec3 col = mix(uColorA, vec3(1.0), 0.4) * m;
    fragColor = vec4(col * uIntensity * 0.9, 1.0);
}
