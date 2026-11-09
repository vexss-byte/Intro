// Nebula backdrop for the void sky. Drawn with a "screen" blend so it only shows on dark pixels.
void main() {
    vec2 p = vUv * vec2(uRes.x / uRes.y, 1.0) * 2.2;
    float t = uTime * 0.02;
    float n1 = fbm(p + vec2(t, -t * 0.6));
    float n2 = fbm(p * 1.7 - vec2(t * 0.8, t) + 5.0);
    vec3 col = mix(vec3(0.02, 0.05, 0.16), vec3(0.05, 0.38, 0.72), smoothstep(0.35, 0.85, n1));
    col += vec3(0.28, 0.08, 0.42) * smoothstep(0.45, 0.9, n2) * 0.6;
    float star = step(0.9975, hash21(floor(vUv * uRes / 2.0)));
    col += star * (0.5 + 0.5 * sin(uTime * 3.0 + hash21(floor(vUv * uRes / 2.0)) * 40.0));
    fragColor = vec4(col * uIntensity, 1.0);
}
