package dev.the_fireplace.textbook.mixin;

import com.google.common.collect.Lists;
import dev.the_fireplace.textbook.TextbookConstants;
import dev.the_fireplace.textbook.usecase.ImportBook;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.SelectionManager;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(BookEditScreen.class)
public abstract class BookEditScreenMixin extends Screen {
	@Mutable
	@Shadow @Final private List<String> pages;

	@Shadow private String title;

	@Shadow private boolean dirty;

	@Shadow protected abstract void removeEmptyPages();

	@Shadow protected abstract void invalidatePageContent();

	@Shadow private boolean signing;

	@Shadow protected abstract void updateButtons();

	@Shadow @Final private SelectionManager currentPageSelectionManager;
	@Shadow private int currentPage;
	private ButtonWidget importButton;
	private ButtonWidget importClipboardButton;
	private ButtonWidget volumeConfirmButton;
	private static int selectedVolume = 1;
	private ButtonWidget upArrow;
	private ButtonWidget downArrow;
	private boolean textbookButtonsInitialized = false;

	protected BookEditScreenMixin(Text title) {
		super(title);
	}

	@Inject(at = @At("TAIL"), method = "init")
	private void init(CallbackInfo info) {
		importClipboardButton = this.addDrawableChild(new ButtonWidget(this.width / 2 - 120, 196 + 20 + 2, 118, 20, Text.translatable("gui.textbook.import_clip"), this::importClipboardText));
		volumeConfirmButton = this.addDrawableChild(new ButtonWidget(this.width / 2 + 100 + 2, 196 + 20 + 2, 118, 20, Text.translatable("gui.textbook.volume_confirm", selectedVolume, (int)Math.ceil(pages.size() / 100d)), this::confirmVolumeSelection));
		upArrow = this.addDrawableChild(new ButtonWidget(this.width / 2 + 100 + 2, 196 + 2, 20, 20, Text.of("^"), (buttonWidget) -> {
			selectedVolume++;
			updateButtons();
		}));
		downArrow = this.addDrawableChild(new ButtonWidget(this.width / 2 + 100 + 2, 196 + 40 + 2, 20, 20, Text.of("v"), (buttonWidget) -> {
			selectedVolume--;
			updateButtons();
		}));
		textbookButtonsInitialized = true;
		updateButtons();
	}

	@Inject(at = @At("TAIL"), method = "updateButtons")
	private void updateButtons(CallbackInfo ci) {
		if (textbookButtonsInitialized) {
			volumeConfirmButton.setMessage(Text.translatable("gui.textbook.volume_confirm", selectedVolume, (int)Math.ceil(pages.size() / 100d)));
			importButton.visible = importClipboardButton.visible = !this.signing;
			int totalVolumeCount = (int) Math.ceil(pages.size() / 100d);
			boolean showVolumeSelector = !this.signing && totalVolumeCount > 1;
			upArrow.visible = downArrow.visible = volumeConfirmButton.visible = showVolumeSelector;
			if (showVolumeSelector) {
				if (selectedVolume > totalVolumeCount) {
					selectedVolume = 1;
				}
				upArrow.active = selectedVolume < totalVolumeCount;
				downArrow.active = selectedVolume > 1;
			}
		}
	}

	private void confirmVolumeSelection(ButtonWidget buttonWidget) {
		int maxVolumeDigits = String.valueOf((int) Math.ceil(pages.size() / 100d)).length();
		setPages(Lists.partition(this.pages, 100).get(selectedVolume - 1));
		appendVolumeToTitle(maxVolumeDigits);
		updateButtons();
	}

	private void appendVolumeToTitle(int digits) {
		if (title.isEmpty()) {
			return;
		}
		if (digits > 15) {
			TextbookConstants.getLogger().info("Do you really need to import that much text? That's a {} digit number. How will you even label the volumes accurately?", digits);
			digits = 15;
		}
		if (title.length() > 15 - digits) {
			this.title = title.substring(0, 15 - digits);
		}
		if (!title.isEmpty()) {
			this.title += "-" + String.format("%0" + digits + "d", selectedVolume);
		}
	}


	private void importClipboardText(ButtonWidget buttonWidget) {
		ImportBook importBook = TextbookConstants.getInjector().getInstance(ImportBook.class);
		ImportBook.Response importedData = importBook.importBookFromClipboard();
		processImportResponse(importedData);
	}

	private void processImportResponse(ImportBook.Response response) {
		if (!response.success()) {
			return;
		}
		if (!response.title().isBlank()) {
			this.title = response.title();
		}
		this.setPages(response.pages());
	}

	private void setPages(List<String> pages) {
		this.currentPage = 0;
		this.pages = pages;
		this.removeEmptyPages();
		this.currentPageSelectionManager.putCursorAtEnd();
		this.dirty = true;
		this.invalidatePageContent();
		updateButtons();
	}
}
