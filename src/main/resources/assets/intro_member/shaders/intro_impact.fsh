// Expanding shock ring when a member is revealed (additive). uIntensity decays from 1 to 0.
void main() {
    vec2 d = fromCenter(vUv);
    float r = length(d);
    float radius = (1.0 - uIntensity) * 0.9 + 0.05;
    float ring = smoothstep(0.06, 0.0, abs(r - radius));
    float flash = exp(-r * r * 14.0) * uIntensity * uIntensity;
    vec3 col = mix(uColorA, vec3(1.0), 0.5) * (ring * uIntensity + flash * 0.8);
    fragColor = vec4(col, 1.0);
}
