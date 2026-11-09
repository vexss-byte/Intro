// uMode 0: bright-pass + blur of the scene into a half-res target. uMode 1: draw that target (added on top).
uniform sampler2D uScene;
uniform int uMode;

vec3 bright(vec2 uv) {
    vec3 c = texture(uScene, uv).rgb;
    float l = dot(c, vec3(0.2126, 0.7152, 0.0722));
    return c * smoothstep(0.62, 1.0, l);
}

void main() {
    if (uMode == 1) { fragColor = vec4(texture(uScene, vUv).rgb * uIntensity, 1.0); return; }
    vec2 px = 1.0 / uRes;
    vec3 sum = vec3(0.0);
    float wsum = 0.0;
    for (int x = -3; x <= 3; x++) {
        for (int y = -3; y <= 3; y++) {
            float w = exp(-float(x * x + y * y) / 6.0);
            sum += bright(vUv + vec2(x, y) * px * 2.0) * w;
            wsum += w;
        }
    }
    fragColor = vec4(sum / wsum, 1.0);
}
