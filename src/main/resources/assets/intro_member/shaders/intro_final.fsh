// Multiplied onto the finished frame: soft vignette + gentle warm colour grade. Output is a per-pixel multiplier.
uniform float uVignette;
uniform float uGrade;

void main() {
    vec2 d = vUv - 0.5;
    float v = 1.0 - uVignette * smoothstep(0.35, 0.9, length(d * vec2(1.0, 0.85)) * 1.25);
    vec3 grade = mix(vec3(1.0), vec3(1.04, 1.0, 0.94), uGrade);
    fragColor = vec4(grade * v, 1.0);
}
