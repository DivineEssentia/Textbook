package dev.the_fireplace.textbook.config;

import dev.the_fireplace.lib.api.chat.injectables.TranslatorFactory;
import dev.the_fireplace.lib.api.chat.interfaces.Translator;
import dev.the_fireplace.lib.api.lazyio.injectables.ConfigStateManager;
import dev.the_fireplace.textbook.TextbookConstants;
import dev.the_fireplace.textbook.api.ConfigScreenBuilderFactoryNew;
import dev.the_fireplace.textbook.api.ConfigScreenBuilderNew;
import dev.the_fireplace.textbook.domain.config.ConfigValues;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Environment(EnvType.CLIENT)
@Singleton
public final class TextbookConfigScreenFactory
{
    private static final String TRANSLATION_BASE = "text.config." + TextbookConstants.MODID + ".";
    private static final String OPTION_TRANSLATION_BASE = TRANSLATION_BASE + "option.";

    private final Translator translator;
    private final ConfigStateManager configStateManager;
    private final TextbookConfig config;
    private final ConfigValues defaultConfigValues;
    private final ConfigScreenBuilderFactoryNew configScreenBuilderFactory;

    private ConfigScreenBuilderNew configScreenBuilder;

    @Inject
    public TextbookConfigScreenFactory(
        TranslatorFactory translatorFactory,
        ConfigStateManager configStateManager,
        TextbookConfig config,
        @Named("default") ConfigValues defaultConfigValues,
        ConfigScreenBuilderFactoryNew configScreenBuilderFactory
    ) {
        this.translator = translatorFactory.getTranslator(TextbookConstants.MODID);
        this.configStateManager = configStateManager;
        this.config = config;
        this.defaultConfigValues = defaultConfigValues;
        this.configScreenBuilderFactory = configScreenBuilderFactory;
    }

    public Screen getConfigScreen(Screen parent) {
        this.configScreenBuilder = configScreenBuilderFactory.create(
            translator,
            TRANSLATION_BASE + "title",
            TRANSLATION_BASE + "global",
            parent,
            () -> configStateManager.save(config)
        );
        addGlobalCategoryEntries();

        return this.configScreenBuilder.build();
    }

    private void addGlobalCategoryEntries() {
        configScreenBuilder.addIntField(
            OPTION_TRANSLATION_BASE + "pageByteLimit",
            config.getPageByteLimit(),
            defaultConfigValues.getPageByteLimit(),
            config::setPageByteLimit
        ).setMinimum(0).setDescriptionRowCount((byte) 3);
    }
}
