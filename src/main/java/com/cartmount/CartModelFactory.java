package com.cartmount;

import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.JagexColor;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.api.NPCComposition;
import net.runelite.api.gameval.ObjectID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class CartModelFactory
{
    static final int MINECART_OBJECT_ID = ObjectID.MINECART;
    private static final int OBJECT_CONFIG_ARCHIVE = 6;
    private static final int DEFAULT_OBJECT_TYPE = 10;
    private static final Logger log = LoggerFactory.getLogger(CartModelFactory.class);

    private final Client client;
    private final Map<MountType, Model> baseModels = new EnumMap<>(MountType.class);

    @Inject
    CartModelFactory(Client client)
    {
        this.client = client;
    }

    Model create(MountType type, int userScalePercent, boolean recolourMinecart,
        Color bodyColour, Color wheelColour, Color hubColour)
    {
        Model base = baseModels.get(type);
        if (base == null)
        {
            base = type.source == MountSource.OBJECT
                ? loadObjectModel(type.sourceId, recolourMinecart, bodyColour, wheelColour, hubColour)
                : loadNpcModel(type.sourceId);
            if (base != null)
            {
                baseModels.put(type, base);
            }
        }
        if (base == null)
        {
            return null;
        }

        int percent = Math.max(1, type.modelScale * userScalePercent / 100);
        if (percent == 100)
        {
            return base;
        }
        Model scaled = client.mergeModels(base);
        if (scaled == null)
        {
            return null;
        }
        int scale = Math.max(1, percent * 128 / 100);
        scaled.scale(scale, scale, scale);
        return ModelSafetyValidator.isSafe(scaled) ? scaled : null;
    }

    void clear()
    {
        baseModels.clear();
    }

    private Model loadObjectModel(int objectId, boolean recolour,
        Color bodyColour, Color wheelColour, Color hubColour)
    {
        try
        {
            byte[] bytes = client.getIndexConfig().loadData(OBJECT_CONFIG_ARCHIVE, objectId);
            if (bytes == null)
            {
                return null;
            }
            ObjectDefinitionData definition = ObjectDefinitionDecoder.decode(bytes);
            if (!definition.isSafeForCustomRendering())
            {
                log.debug("Unsupported mount object definition {}", objectId);
                return null;
            }

            List<ModelData> parts = loadObjectParts(definition, true);
            if (parts.isEmpty())
            {
                // Some scenery such as rugs is encoded as a ground-decoration type.
                parts = loadObjectParts(definition, false);
            }
            if (parts.isEmpty())
            {
                return null;
            }
            ModelData data = merge(parts);
            if (data == null)
            {
                return null;
            }
            applyObjectTransforms(data, definition);
            if (recolour && bodyColour != null && wheelColour != null && hubColour != null)
            {
                applyColours(data, bodyColour, wheelColour, hubColour);
            }
            Model model = data.light(64 + definition.ambient, 768 + definition.contrast, -50, -10, -50);
            return ModelSafetyValidator.isSafe(model) ? model : null;
        }
        catch (RuntimeException | AssertionError ex)
        {
            log.debug("Unable to build mount object " + objectId, ex);
            return null;
        }
    }

    private static void applyColours(ModelData data, Color bodyColour, Color wheelColour, Color hubColour)
    {
        short body = JagexColor.rgbToHSL(bodyColour.getRGB(), 1.0);
        short wheel = JagexColor.rgbToHSL(wheelColour.getRGB(), 1.0);
        short hub = JagexColor.rgbToHSL(hubColour.getRGB(), 1.0);
        short[] faceColours = data.getFaceColors();
        if (faceColours == null)
        {
            return;
        }
        float[] zs = data.getVerticesZ();
        int[] a = data.getFaceIndices1();
        int[] b = data.getFaceIndices2();
        int[] c = data.getFaceIndices3();
        short[] originals = faceColours.clone();
        for (int i = 0; i < faceColours.length; i++)
        {
            if ((originals[i] & 0xffff) != 20)
            {
                faceColours[i] = body;
                continue;
            }
            float centroidZ = (zs[a[i]] + zs[b[i]] + zs[c[i]]) / 3f;
            faceColours[i] = Math.abs(centroidZ) >= 55.5f ? hub : wheel;
        }
    }

    private List<ModelData> loadObjectParts(ObjectDefinitionData definition, boolean requireDefaultType)
    {
        List<ModelData> parts = new ArrayList<>();
        for (int i = 0; i < definition.modelIds.length; i++)
        {
            if (requireDefaultType && definition.modelTypes != null
                && definition.modelTypes[i] != DEFAULT_OBJECT_TYPE)
            {
                continue;
            }
            ModelData part = copyModelData(definition.modelIds[i]);
            if (part == null)
            {
                parts.clear();
                return parts;
            }
            if (definition.rotated)
            {
                part.rotateY180Ccw();
            }
            parts.add(part);
        }
        return parts;
    }

    private Model loadNpcModel(int npcId)
    {
        try
        {
            NPCComposition composition = client.getNpcDefinition(npcId);
            if (composition == null)
            {
                return null;
            }
            if (composition.getConfigs() != null)
            {
                NPCComposition transformed = composition.transform();
                if (transformed != null)
                {
                    composition = transformed;
                }
            }
            int[] ids = composition.getModels();
            if (ids == null || ids.length == 0 || ids.length > 32)
            {
                return null;
            }
            List<ModelData> parts = new ArrayList<>(ids.length);
            for (int id : ids)
            {
                ModelData part = copyModelData(id);
                if (part == null)
                {
                    return null;
                }
                parts.add(part);
            }
            ModelData data = merge(parts);
            if (data == null)
            {
                return null;
            }
            short[] from = composition.getColorToReplace();
            short[] to = composition.getColorToReplaceWith();
            if (from != null && to != null)
            {
                for (int i = 0; i < Math.min(from.length, to.length); i++)
                {
                    data.recolor(from[i], to[i]);
                }
            }
            int width = composition.getWidthScale();
            int height = composition.getHeightScale();
            if (width != 128 || height != 128)
            {
                data.scale(width, height, width);
            }
            Model model = data.light();
            return ModelSafetyValidator.isSafe(model) ? model : null;
        }
        catch (RuntimeException | AssertionError ex)
        {
            log.debug("Unable to build mount NPC " + npcId, ex);
            return null;
        }
    }

    private ModelData copyModelData(int modelId)
    {
        ModelData source = client.loadModelData(modelId);
        if (source == null)
        {
            return null;
        }
        ModelData copy = source.shallowCopy().cloneVertices().cloneColors();
        if (copy.getFaceTextures() != null)
        {
            copy = copy.cloneTextures();
        }
        if (copy.getFaceTransparencies() != null)
        {
            copy = copy.cloneTransparencies();
        }
        return copy;
    }

    private ModelData merge(List<ModelData> parts)
    {
        return parts.size() == 1 ? parts.get(0)
            : client.mergeModels(parts.toArray(new ModelData[0]));
    }

    private static void applyObjectTransforms(ModelData data, ObjectDefinitionData definition)
    {
        if (definition.recolorFrom != null)
        {
            for (int i = 0; i < definition.recolorFrom.length; i++)
            {
                data.recolor(definition.recolorFrom[i], definition.recolorTo[i]);
            }
        }
        if (definition.retextureFrom != null)
        {
            for (int i = 0; i < definition.retextureFrom.length; i++)
            {
                data.retexture(definition.retextureFrom[i], definition.retextureTo[i]);
            }
        }
        if (definition.scaleX != 128 || definition.scaleHeight != 128 || definition.scaleY != 128)
        {
            data.scale(definition.scaleX, definition.scaleHeight, definition.scaleY);
        }
        if (definition.offsetX != 0 || definition.offsetHeight != 0 || definition.offsetY != 0)
        {
            data.translate(definition.offsetX, definition.offsetHeight, definition.offsetY);
        }
    }
}