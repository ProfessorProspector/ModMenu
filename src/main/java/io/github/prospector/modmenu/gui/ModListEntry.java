package io.github.prospector.modmenu.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.prospector.modmenu.ModMenu;
import io.github.prospector.modmenu.util.BadgeRenderer;
import io.github.prospector.modmenu.util.HardcodedUtil;
import io.github.prospector.modmenu.util.RenderUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class ModListEntry extends AlwaysSelectedEntryListWidget.Entry<ModListEntry> {
	public static final Identifier UNKNOWN_ICON = new Identifier("textures/misc/unknown_pack.png");
	private static final Logger LOGGER = LogManager.getLogger();

	protected final MinecraftClient client;
	protected final ModContainer container;
	protected final ModMetadata metadata;
	protected final ModListWidget list;
	protected Identifier iconLocation;

	public ModListEntry(ModContainer container, ModListWidget list) {
		this.container = container;
		this.list = list;
		this.metadata = container.getMetadata();
		this.client = MinecraftClient.getInstance();
	}

	@Override
	public void render(MatrixStack matrices, int index, int y, int x, int rowWidth, int rowHeight, int mouseX, int mouseY, boolean isSelected, float delta) {
		x += getXOffset();
		rowWidth -= getXOffset();
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		this.bindIconTexture();
		RenderSystem.enableBlend();
		DrawableHelper.drawTexture(matrices, x, y, 0.0F, 0.0F, 32, 32, 32, 32);
		RenderSystem.disableBlend();
		Text name = HardcodedUtil.formatFabricModuleName(metadata.getName());
		StringVisitable trimmedName = name;
		int maxNameWidth = rowWidth - 32 - 3;
		TextRenderer font = this.client.textRenderer;
		if (font.getWidth(name) > maxNameWidth) {
			StringVisitable ellipsis = StringVisitable.plain("...");
			trimmedName = StringVisitable.concat(font.trimToWidth(name, maxNameWidth - font.getWidth(ellipsis)), ellipsis);
		}
		font.draw(matrices, Language.getInstance().reorder(trimmedName), x + 32 + 3, y + 1, 0xFFFFFF);
		new BadgeRenderer(x + 32 + 3 + font.getWidth(name) + 2, y, x + rowWidth, container, list.getParent()).draw(matrices, mouseX, mouseY);
		String description = metadata.getDescription();
		if (description.isEmpty() && HardcodedUtil.getHardcodedDescriptions().containsKey(metadata.getId())) {
			description = HardcodedUtil.getHardcodedDescription(metadata.getId());
		}
		RenderUtils.drawWrappedString(matrices, description, (x + 32 + 3 + 4), (y + client.textRenderer.fontHeight + 2), rowWidth - 32 - 7, 2, 0x808080);
	}

	private NativeImageBackedTexture createIcon() {
		try {
			Path path = container.getPath(metadata.getIconPath(64 * MinecraftClient.getInstance().options.guiScale).orElse("assets/" + metadata.getId() + "/icon.png"));
			NativeImageBackedTexture cached = this.list.getCachedModIcon(path);
			if (cached != null) {
				return cached;
			}
			if (!Files.exists(path)) {
				ModContainer modMenu = FabricLoader.getInstance().getModContainer(ModMenu.MOD_ID).orElseThrow(IllegalAccessError::new);
				if (HardcodedUtil.modHasHardcodedIcon(metadata)) {
					path = HardcodedUtil.getHardcodedIcon(metadata);
				} else {
					path = modMenu.getPath("assets/" + ModMenu.MOD_ID + "/grey_fabric_icon.png");
				}
			}
			cached = this.list.getCachedModIcon(path);
			if (cached != null) {
				return cached;
			}
			try (InputStream inputStream = Files.newInputStream(path)) {
				NativeImage image = NativeImage.read(Objects.requireNonNull(inputStream));
				Validate.validState(image.getHeight() == image.getWidth(), "Must be square icon");
				NativeImageBackedTexture tex = new NativeImageBackedTexture(image);
				this.list.cacheModIcon(path, tex);
				return tex;
			}

		} catch (Throwable t) {
			LOGGER.error("Invalid icon for mod {}", this.container.getMetadata().getName(), t);
			return null;
		}
	}

	@Override
	public boolean mouseClicked(double v, double v1, int i) {
		list.select(this);
		return true;
	}

	public ModMetadata getMetadata() {
		return metadata;
	}

	public void bindIconTexture() {
		if (this.iconLocation == null) {
			this.iconLocation = new Identifier("modmenu", metadata.getId() + "_icon");
			NativeImageBackedTexture icon = this.createIcon();
			if (icon != null) {
				this.client.getTextureManager().registerTexture(this.iconLocation, icon);
			} else {
				this.iconLocation = UNKNOWN_ICON;
			}
		}
		this.client.getTextureManager().bindTexture(this.iconLocation);
	}

	public int getXOffset() {
		return 0;
	}
}
