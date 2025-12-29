#version 330 core

#ifndef MAX_TEXTURE_LOD_BIAS
#define MAX_TEXTURE_LOD_BIAS 0.0
#endif

/*
    original credits goes to sodium for most of the logic in this shader
    and the other ones, we have just modded this one to fit our needs
*/

/* we could just use sodiums includes, but this gives us full control */
#import <sodium:include/fog.glsl>
#import <sodium:include/chunk_material.glsl>

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

    return u_UseRGSS ? textureLod(sampler, snappedUV, 0.0) : texture(sampler, snappedUV);
}

void main() {
    vec4 color = sampleNearest(u_BlockTex, v_TexCoord, u_TexelSize);

    /* apply per-vertex color modulator */
    color *= v_Color;

    #ifdef USE_FRAGMENT_DISCARD
    if (color.a < _material_alpha_cutoff(v_Material)) {
        fragColor = vec4(0.0);
        discard;
    }
    #endif

    fragColor = _linearFog(color, v_FragDistance, u_FogColor, u_EnvironmentFog, u_RenderFog, fadeFactor);
}