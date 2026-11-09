// Shared by every intro_member fragment shader. "#version 330 core" is prepended by ShaderManager.
in vec2 vUv;
out vec4 fragColor;

uniform float uTime;        // seconds of cinematic time
uniform vec2  uRes;         // target size in pixels
uniform float uIntensity;   // 0..1 strength of the effect
uniform vec3  uColorA;      // primary colour (current member / gold)
uniform vec3  uColorB;      // secondary colour (orange)
uniform vec2  uCenter;      // focus in 0..1 screen space, y down

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float vnoise(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash21(i), b = hash21(i + vec2(1, 0)), c = hash21(i + vec2(0, 1)), d = hash21(i + vec2(1, 1));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm(vec2 p) {
    float v = 0.0, a = 0.5;
    for (int i = 0; i < 5; i++) { v += a * vnoise(p); p = p * 2.03 + 17.1; a *= 0.5; }
    return v;
}

// aspect-corrected offset from the focus, y up
vec2 fromCenter(vec2 uv) {
    vec2 d = uv - vec2(uCenter.x, 1.0 - uCenter.y);
    d.x *= uRes.x / uRes.y;
    return d;
}
