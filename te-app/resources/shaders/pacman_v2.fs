#define TE_ALPHATHRESHOLD 0.96

const float PI = 3.14159265359;
const float TAU = 6.28318530718;

uniform float mouthSize;
uniform float mouthOpen;
uniform bool showEye;
uniform float eyeSize;
uniform vec2 eyeOffset;
uniform bool faceRight;
uniform float edgeFeather;

float circleMask(vec2 p, float radius, float feather) {
    return smoothstep(radius + feather, radius - feather, length(p));
}

float angleDiff(float a, float b) {
    float d = abs(a - b);
    return min(d, TAU - d);
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 uv = fragCoord / iResolution.xy;
    vec2 p = uv - 0.5;
    p.x *= iResolution.x / iResolution.y;

    float radius = 0.48 * iScale;
    float feather = max(edgeFeather * 0.35, 0.0006);
    float cutoutFeather = max(edgeFeather * 0.08, 0.00025);

    float bodyMask = circleMask(p, radius, feather);

    float facing = faceRight ? 0.0 : PI;
    float mouthHalfAngle = mouthSize * PI * mouthOpen;
    float angle = atan(p.y, p.x);
    float mouthAngle = angleDiff(angle, facing);
    float mouthAngularFeather = 0.012 + cutoutFeather * 1.2;
    float mouthMask =
        smoothstep(mouthHalfAngle + mouthAngularFeather, mouthHalfAngle - mouthAngularFeather, mouthAngle)
        * bodyMask;

    float finalMask = bodyMask * (1.0 - mouthMask);

    if (showEye) {
        vec2 eyeCenter = vec2((faceRight ? -1.0 : 1.0) * eyeOffset.x * radius, eyeOffset.y * radius);
        float eyeRadius = eyeSize * radius;
        float eyeMask = circleMask(p - eyeCenter, eyeRadius, cutoutFeather);
        finalMask *= (1.0 - eyeMask);
    }

    finalMask = clamp(finalMask, 0.0, 1.0);

    fragColor = vec4(iColorRGB, finalMask);
}
