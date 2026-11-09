// Soft animated glow behind the featured member (additive).
void main() {
    vec2 d = fromCenter(vUv);
    float r = length(d);
    float pulse = 0.85 + 0.15 * sin(uTime * 3.2);
    float g = exp(-r * r * 9.0) * pulse;
    float halo = exp(-r * 2.4) * 0.25;
    vec3 col = uColorA * g * 0.55 + uColorB * halo * 0.35;
    fragColor = vec4(col * uIntensity, 1.0);
}
