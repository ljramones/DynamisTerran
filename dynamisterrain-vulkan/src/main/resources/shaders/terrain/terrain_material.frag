#version 450

float sampleHorizonAngle(sampler2D horizonMap, vec2 uv, float sunAzimuth) {
    float dirF   = mod(sunAzimuth / (2.0 * 3.14159265), 1.0) * 8.0;
    int dir0     = int(dirF) % 8;
    int dir1     = (dir0 + 1) % 8;
    float t      = fract(dirF);

    vec4 packed = texture(horizonMap, uv);
    ivec4 bytes = ivec4(round(packed * 255.0));

    int nibbleForDir0;
    int nibbleForDir1;

    if (dir0 == 0) nibbleForDir0 = (bytes.r >> 4) & 0xF;
    else if (dir0 == 1) nibbleForDir0 = bytes.r & 0xF;
    else if (dir0 == 2) nibbleForDir0 = (bytes.g >> 4) & 0xF;
    else if (dir0 == 3) nibbleForDir0 = bytes.g & 0xF;
    else if (dir0 == 4) nibbleForDir0 = (bytes.b >> 4) & 0xF;
    else if (dir0 == 5) nibbleForDir0 = bytes.b & 0xF;
    else if (dir0 == 6) nibbleForDir0 = (bytes.a >> 4) & 0xF;
    else nibbleForDir0 = bytes.a & 0xF;

    if (dir1 == 0) nibbleForDir1 = (bytes.r >> 4) & 0xF;
    else if (dir1 == 1) nibbleForDir1 = bytes.r & 0xF;
    else if (dir1 == 2) nibbleForDir1 = (bytes.g >> 4) & 0xF;
    else if (dir1 == 3) nibbleForDir1 = bytes.g & 0xF;
    else if (dir1 == 4) nibbleForDir1 = (bytes.b >> 4) & 0xF;
    else if (dir1 == 5) nibbleForDir1 = bytes.b & 0xF;
    else if (dir1 == 6) nibbleForDir1 = (bytes.a >> 4) & 0xF;
    else nibbleForDir1 = bytes.a & 0xF;

    float a0 = (float(nibbleForDir0) / 15.0) * (3.14159265 * 0.5);
    float a1 = (float(nibbleForDir1) / 15.0) * (3.14159265 * 0.5);
    return mix(a0, a1, t);
}
