// Swirling energy ribbons around the focus (additive).
void main() {
    vec2 d = fromCenter(vUv);
    float r = length(d), a = atan(d.y, d.x);
    float ribbons = 0.0;
    for (int i = 0; i < 3; i++) {
        float ph = float(i) * 2.094 + uTime * (0.9 + float(i) * 0.3);
        float w = abs(sin(a * 3.0 + r * 9.0 - ph));
        ribbons += pow(1.0 - w, 14.0);
    }
    float falloff = smoothstep(0.55, 0.05, r) * smoothstep(0.0, 0.08, r);
    float n = fbm(d * 7.0 + uTime * 0.6);
    vec3 col = mix(uColorB, uColorA, n) * ribbons * falloff;
    fragColor = vec4(col * uIntensity * 0.9, 1.0);
}
