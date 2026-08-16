const float PI = 3.14159265359;
const float TAU = 6.28318530718;

uniform float mouthSize;
uniform float mouthOpen;
uniform bool showEye;
uniform float eyeSize;
uniform vec2 eyeOffset;
uniform bool faceRight;

float angleDiff(float a, float b) {
    float d = abs(a - b);
    return min(d, TAU - d);
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 uv = fragCoord / iResolution.xy;
    vec2 p = uv - 0.5;
    p.x *= iResolution.x / iResolution.y;

    float radius = 0.48 * iScale;
    float bodyMask = 1.0 - step(radius, length(p));

    float facing = faceRight ? 0.0 : PI;
    float mouthHalfAngle = mouthSize * PI * mouthOpen;
    float angle = atan(p.y, p.x);
    float mouthAngle = angleDiff(angle, facing);
    float mouthMask = (1.0 - step(mouthHalfAngle, mouthAngle)) * bodyMask;

    float finalMask = bodyMask * (1.0 - mouthMask);

    if (showEye) {
        vec2 eyeCenter = vec2((faceRight ? -1.0 : 1.0) * eyeOffset.x * radius, eyeOffset.y * radius);
        float eyeRadius = eyeSize * radius;
        float eyeMask = 1.0 - step(eyeRadius, length(p - eyeCenter));
        finalMask *= (1.0 - eyeMask);
    }

    finalMask = step(0.5, finalMask);
    fragColor = vec4(iColorRGB, finalMask);
}
