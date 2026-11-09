package dev.introcinema.client.render;

import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;

/**
 * Black "???" look for hidden members, applied to the render state only (the real player is untouched).
 * The nameplate is always removed. Skin swap and item hiding use reflection so that a signature difference
 * in this Minecraft version degrades to "name hidden only" instead of a crash.
 */
public final class SilhouetteRender {
    private static final Logger LOG = LoggerFactory.getLogger("intro_member/silhouette");
    private static List<Field> itemFields, itemStateFields;
    private static Object blackTexture;
    private static boolean skinBroken;

    private SilhouetteRender() {}

    public static void apply(AvatarRenderState state) {
        state.nameTag = null;
        state.hasRedOverlay = false;
        try {
            hideItems(state);
            swapSkin(state);
        } catch (Throwable t) {
            if (!skinBroken) LOG.warn("Silhouette is partial (name hidden only): {}", t.toString());
            skinBroken = true;
        }
    }

    private static void hideItems(AvatarRenderState s) throws Exception {
        if (itemFields == null) {
            itemFields = new ArrayList<>(); itemStateFields = new ArrayList<>();
            for (Class<?> c = s.getClass(); c != null && c != Object.class; c = c.getSuperclass())
                for (Field f : c.getDeclaredFields()) {
                    if (Modifier.isStatic(f.getModifiers())) continue;
                    if (f.getType() == ItemStack.class && !Modifier.isFinal(f.getModifiers())) { f.setAccessible(true); itemFields.add(f); }
                    else if (f.getType().getSimpleName().equals("ItemStackRenderState")) { f.setAccessible(true); itemStateFields.add(f); }
                }
        }
        for (Field f : itemFields) f.set(s, ItemStack.EMPTY);
        for (Field f : itemStateFields) {
            Object o = f.get(s);
            if (o == null) continue;
            try { Method m = o.getClass().getMethod("clear"); m.invoke(o); } catch (NoSuchMethodException ignored) {}
        }
    }

    private static void swapSkin(AvatarRenderState s) throws Exception {
        if (blackTexture == null) blackTexture = makeTexture();
        PlayerSkin old = s.skin;
        RecordComponent[] rc = PlayerSkin.class.getRecordComponents();
        Class<?>[] types = new Class<?>[rc.length];
        Object[] args = new Object[rc.length];
        for (int i = 0; i < rc.length; i++) {
            types[i] = rc[i].getType();
            Method acc = rc[i].getAccessor();
            args[i] = rc[i].getName().equals("body") ? blackTexture : acc.invoke(old);
            if (rc[i].getName().equals("cape") || rc[i].getName().equals("elytra")) args[i] = null;
        }
        Constructor<PlayerSkin> ctor = PlayerSkin.class.getDeclaredConstructor(types);
        ctor.setAccessible(true);
        s.skin = ctor.newInstance(args);
    }

    private static Object makeTexture() throws Exception {
        Class<?> cls = Class.forName("net.minecraft.core.ClientAsset$ResourceTexture");
        Identifier id = Identifier.fromNamespaceAndPath("intro_member", "entity/silhouette");
        Identifier path = Identifier.fromNamespaceAndPath("intro_member", "textures/entity/silhouette.png");
        for (Constructor<?> c : cls.getDeclaredConstructors()) {
            Class<?>[] p = c.getParameterTypes();
            c.setAccessible(true);
            if (p.length == 2 && p[0] == Identifier.class && p[1] == Identifier.class) return c.newInstance(id, path);
            if (p.length == 1 && p[0] == Identifier.class) return c.newInstance(id);
        }
        throw new IllegalStateException("no usable ResourceTexture constructor");
    }
}
