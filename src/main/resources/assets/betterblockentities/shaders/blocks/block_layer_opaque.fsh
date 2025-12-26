#version 330 core

#ifndef MAX_TEXTURE_LOD_BIAS
#error "MAX_TEXTURE_LOD_BIAS constant not specified"
#endif

/*
    original credits goes to sodium for most of the logic in this shader
    and the other ones, we have just modded this one to fit our needs
*/

/* we could just use sodiums includes, but this gives us full control */
#import <betterblockentities:include/fog.glsl>
#import <betterblockentities:include/chunk_material.glsl>

/* the interpolated vertex color */
in vec4 v_Color;

/* the interpolated block texture coordinates */
in vec2 v_TexCoord;

/* the fragment's distance from the camera (cylindrical and spherical) */
in vec2 v_FragDistance;

in float fadeFactor;
flat in uint v_Material;

/* the block texture */
uniform sampler2D u_BlockTex;

/* the color of the shader fog */
uniform vec4 u_FogColor;

/* the start and end position for environmental fog */
uniform vec2 u_EnvironmentFog;

uniform vec2 u_RenderFog;
uniform vec2 u_TexelSize;
uniform bool u_UseRGSS;

/* the output fragment for the color framebuffer */
out vec4 fragColor;

/* fastest possible nearest-texel sampling */
vec4 sampleNearest(sampler2D sampler, vec2 uv, vec2 pixelSize) {
    /* snap to texel center */
    vec2 snappedUV = (floor(uv / pixelSize) + 0.5) * pixelSize;

    return texture(sampler, snappedUV);
}

vec4 sampleNearest(sampler2D source, vec2 uv, vec2 pixelSize) {
    return sampleNearest(source, uv, pixelSize);
}

/* rotated grid super-sampling */
vec4 sampleRGSS(sampler2D source, vec2 uv, vec2 pixelSize) {
    vec2 du = dFdx(uv);
    vec2 dv = dFdy(uv);

    vec2 texelScreenSize = sqrt(du * du + dv * dv);
    float maxTexelSize = max(texelScreenSize.x, texelScreenSize.y);

    float minPixelSize = min(pixelSize.x, pixelSize.y);

    float transitionStart = minPixelSize * 1.0;
    float transitionEnd = minPixelSize * 2.0;
    float blendFactor = smoothstep(transitionStart, transitionEnd, maxTexelSize);

    float duLength = length(du);
    float dvLength = length(dv);
    float minDerivative = min(duLength, dvLength);
    float maxDerivative = max(duLength, dvLength);

    float effectiveDerivative = sqrt(minDerivative * maxDerivative);

    float mipLevelExact = max(0.0, log2(effectiveDerivative / minPixelSize));

    float mipLevelLow = floor(mipLevelExact);
    float mipLevelHigh = mipLevelLow + 1.0;
    float mipBlend = fract(mipLevelExact);

    const vec2 offsets[4] = vec2[](
    vec2(0.125, 0.375),
    vec2(-0.125, -0.375),
    vec2(0.375, -0.125),
    vec2(-0.375, 0.125)
    );

    vec4 rgssColorLow = vec4(0.0);
    vec4 rgssColorHigh = vec4(0.0);
    for (int i = 0; i < 4; ++i) {
        vec2 sampleUV = uv + offsets[i] * pixelSize;
        rgssColorLow += textureLod(source, sampleUV, mipLevelLow);
        rgssColorHigh += textureLod(source, sampleUV, mipLevelHigh);
    }
    rgssColorLow *= 0.25;
    rgssColorHigh *= 0.25;

    vec4 rgssColor = mix(rgssColorLow, rgssColorHigh, mipBlend);

    vec4 nearestColor = sampleNearest(source, uv, pixelSize, du, dv, texelScreenSize);

    return mix(nearestColor, rgssColor, blendFactor);
}

void main() {
    /* decide method, in our case always sampleNearest */
    vec4 color = u_UseRGSS ? sampleRGSS(u_BlockTex, v_TexCoord, u_TexelSize) : sampleNearest(u_BlockTex, v_TexCoord, u_TexelSize);

    /* apply per-vertex color modulator */
    color *= v_Color;

    #ifdef USE_FRAGMENT_DISCARD
    if (color.a < _material_alpha_cutoff(v_Material)) {
        discard;
    }
    #endif

    fragColor = _linearFog(color, v_FragDistance, u_FogColor, u_EnvironmentFog, u_RenderFog, fadeFactor);
}