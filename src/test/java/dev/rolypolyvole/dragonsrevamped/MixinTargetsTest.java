package dev.rolypolyvole.dragonsrevamped;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertFalse;

class MixinTargetsTest {

    private static final String MIXIN_CONFIG = "dragons-revamped.mixins.json";
    private static final String MIXIN_ANNOTATION = "Lorg/spongepowered/asm/mixin/Mixin;";

    @Test
    void mixinsApplyToTheirTargets() throws IOException, ClassNotFoundException {
        ClassLoader loader = getClass().getClassLoader();
        List<String> targets = new ArrayList<>();

        JsonObject config = JsonParser.parseReader(new InputStreamReader(open(loader, MIXIN_CONFIG))).getAsJsonObject();
        String mixinPackage = config.get("package").getAsString();

        for (JsonElement mixin : config.getAsJsonArray("mixins")) {
            targets.addAll(readTargets(loader, mixinPackage + "." + mixin.getAsString()));
        }

        assertFalse(targets.isEmpty(), "No mixin targets found");

        for (String target : targets) {
            Class.forName(target, false, loader);
        }
    }

    private static List<String> readTargets(ClassLoader loader, String mixinClass) throws IOException {
        ClassNode node = new ClassNode();

        try (InputStream bytes = open(loader, mixinClass.replace('.', '/') + ".class")) {
            new ClassReader(bytes).accept(node, ClassReader.SKIP_CODE);
        }

        List<String> targets = new ArrayList<>();

        for (AnnotationNode annotation : Objects.requireNonNullElse(node.invisibleAnnotations, List.<AnnotationNode>of())) {
            if (!annotation.desc.equals(MIXIN_ANNOTATION)) continue;

            for (int i = 0; i < annotation.values.size(); i += 2) {
                String key = (String) annotation.values.get(i);

                for (Object value : (List<?>) annotation.values.get(i + 1)) {
                    if (key.equals("value")) targets.add(((Type) value).getClassName());
                    if (key.equals("targets")) targets.add(((String) value).replace('/', '.'));
                }
            }
        }

        assertFalse(targets.isEmpty(), mixinClass + " has no targets");
        return targets;
    }

    private static InputStream open(ClassLoader loader, String resource) {
        return Objects.requireNonNull(loader.getResourceAsStream(resource), "Missing " + resource);
    }
}
