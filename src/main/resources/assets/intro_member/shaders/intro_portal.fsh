// Portal swirl with a radially distorted ring (additive).
void main() {
    vec2 d = fromCenter(vUv);
    float r = length(d), a = atan(d.y, d.x);
    float swirl = a + 3.0 / (r + 0.15) - uTime * 2.0;
    float arms = pow(abs(sin(swirl * 2.0)), 8.0);
    float ring = smoothstep(0.03, 0.0, abs(r - 0.28 - 0.01 * sin(uTime * 4.0 + a * 6.0)));
    float core = smoothstep(0.3, 0.0, r);
    vec3 col = uColorA * (arms * core * 0.8 + ring * 1.4) + uColorB * core * 0.35;
    fragColor = vec4(col * uIntensity, 1.0);
}
