// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.engine.rendering.iconmesh;

import org.joml.Vector4f;
import org.terasology.gestalt.assets.Asset;
import org.terasology.gestalt.assets.ResourceUrn;
import org.terasology.engine.rendering.primitives.Tessellator;
import org.terasology.engine.rendering.primitives.TessellatorHelper;
import org.terasology.joml.geom.Rectanglei;
import org.terasology.gestalt.module.sandbox.API;
import org.terasology.gestalt.naming.Name;
import org.terasology.engine.rendering.assets.mesh.Mesh;
import org.terasology.engine.rendering.assets.mesh.MeshData;
import org.terasology.engine.rendering.assets.texture.TextureRegion;
import org.terasology.engine.utilities.Assets;

import java.nio.ByteBuffer;

@API
public final class IconMeshFactory {

    private IconMeshFactory() {
    }

    public static Mesh getIconMesh(TextureRegion region) {
        if (region instanceof Asset) {
            ResourceUrn urn = ((Asset<?>) region).getUrn();
            if (urn.getFragmentName().isEmpty()) {
                return Assets.get(new ResourceUrn(urn.getModuleName(), IconMeshDataProducer.ICON_DISCRIMINATOR,
                        urn.getResourceName()), Mesh.class).get();
            } else {
                Name fragName = new Name(urn.getResourceName().toString() + ResourceUrn.FRAGMENT_SEPARATOR + urn.getFragmentName().toString());
                return Assets.get(new ResourceUrn(urn.getModuleName(), IconMeshDataProducer.ICON_DISCRIMINATOR,
                        fragName), Mesh.class).get();
            }
        } else {
            return generateIconMesh(region);
        }
    }

    public static Mesh generateIconMesh(TextureRegion tex) {
        return generateIconMesh(null, tex, 0, false, null);
    }

    public static Mesh generateIconMesh(ResourceUrn urn, TextureRegion tex) {
        return generateIconMesh(urn, tex, 0, false, null);
    }

    public static MeshData generateIconMeshData(TextureRegion tex) {
        return generateIconMeshData(tex, 0, false, null);
    }

    public static Mesh generateIconMesh(ResourceUrn urn, TextureRegion tex, int alphaLimit, boolean withContour, Vector4f colorContour) {
        if (urn == null) {
            return Assets.generateAsset(generateIconMeshData(tex, alphaLimit, withContour, colorContour), Mesh.class);
        } else {
            return Assets.generateAsset(urn, generateIconMeshData(tex, alphaLimit, withContour, colorContour), Mesh.class);
        }
    }

    public static MeshData generateIconMeshData(TextureRegion tex, int alphaLimit, boolean withContour, Vector4f colorContour) {
        ByteBuffer buffer = tex.getTexture().getData().getBuffers()[0];

        Rectanglei pixelRegion = tex.getPixelRegion();
        int posX = pixelRegion.minX;
        int posY = pixelRegion.minY;

        int stride = tex.getTexture().getWidth() * 4;

        float textureSize = Math.max(tex.getWidth(), tex.getHeight());

        Tessellator tessellator = new Tessellator();

        for (int y = 0; y < tex.getHeight(); y++) {
            for (int x = 0; x < tex.getWidth(); x++) {
                int r = buffer.get((posY + y) * stride + (posX + x) * 4) & 255;
                int g = buffer.get((posY + y) * stride + (posX + x) * 4 + 1) & 255;
                int b = buffer.get((posY + y) * stride + (posX + x) * 4 + 2) & 255;
                int a = buffer.get((posY + y) * stride + (posX + x) * 4 + 3) & 255;

                if (a > alphaLimit) {
                    Vector4f color = new Vector4f(r / 255f, g / 255f, b / 255f, a / 255f);
                    TessellatorHelper.addBlockMesh(tessellator, color, 2f / textureSize, 1.0f, 0.5f,
                            2f / textureSize * x - 1f, 2f / textureSize * (tex.getHeight() - y - 1) - 1f, 0f);

                    if (withContour) {
                        int newX = 0;
                        int newY = 0;
                        int newA = 0;

                        for (int i = 0; i < 4; i++) {
                            newA = alphaLimit + 1;
                            switch (i) {
                                case 0:
                                    //check left
                                    if (x > 0) {
                                        newX = x - 1;
                                        newY = y;
                                        newA = buffer.get((posY + newY) * stride + (posX + newX) * 4 + 3) & 255;
                                    }
                                    break;
                                case 1:
                                    //check top
                                    if (y > 0) {
                                        newX = x;
                                        newY = y - 1;
                                        newA = buffer.get((posY + newY) * stride + (posX + newX) * 4 + 3) & 255;
                                    }
                                    break;
                                case 2:
                                    //check right
                                    if (x < 16) {
                                        newX = x + 1;
                                        newY = y;
                                        newA = buffer.get((posY + newY) * stride + (posX + newX) * 4 + 3) & 255;
                                    }
                                    break;
                                case 3:
                                    //check bottom
                                    if (y < 16) {
                                        newX = x;
                                        newY = y + 1;
                                        newA = buffer.get((posY + newY) * stride + (posX + newX) * 4 + 3) & 255;
                                    }
                                    break;
                                default:
                                    break;
                            }

                            if (newA < alphaLimit) {
                                Vector4f cColor = new Vector4f(colorContour.x / 255f,
                                        colorContour.y / 255f,
                                        colorContour.z / 255f, colorContour.w);
                                TessellatorHelper.addBlockMesh(tessellator, cColor, 0.125f, 1.0f, 0.5f,
                                        2f * 0.0625f * newX - 0.5f, 0.125f * (15 - newY) - 1f, 0f);
                            }
                        }
                    }
                }
            }
        }
        return tessellator.buildMeshData();
    }

}
