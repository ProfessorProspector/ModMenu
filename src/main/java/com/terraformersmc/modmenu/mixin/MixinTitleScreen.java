package com.terraformersmc.modmenu.mixin;

import com.terraformersmc.modmenu.ModMenu;
import com.terraformersmc.modmenu.config.ModMenuConfig;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.language.I18n;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(TitleScreen.class)
public class MixinTitleScreen {
	@ModifyArg(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;init(Lnet/minecraft/client/Minecraft;II)V"), method = "init", index = 2)
	private int adjustRealmsHeight(int height) {
		if (ModMenuConfig.MODIFY_TITLE_SCREEN.getValue() && ModMenuConfig.MODS_BUTTON_STYLE.getValue() == ModMenuConfig.ModsButtonStyle.CLASSIC) {
			return height - 51;
		} else if (ModMenuConfig.MODS_BUTTON_STYLE.getValue() == ModMenuConfig.ModsButtonStyle.REPLACE_REALMS || ModMenuConfig.MODS_BUTTON_STYLE.getValue() == ModMenuConfig.ModsButtonStyle.SHRINK) {
			return -99999;
		}
		return height;
	}

	@ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/TitleScreen;drawString(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V", ordinal = 0))
	private String onRender(String string) {
		if (ModMenuConfig.MODIFY_TITLE_SCREEN.getValue() && ModMenuConfig.MOD_COUNT_LOCATION.getValue().isOnTitleScreen()) {
			String count = ModMenu.getDisplayedModCount();
			String newString = I18n.get("modmenu.mods.a", count);
			String countKey = "modmenu.mods." + count;
			if ("69".equals(count) && ModMenuConfig.EASTER_EGGS.getValue()) {
				newString = I18n.get(countKey + ".nice", count);
			} else if (I18n.exists(countKey)) {
				newString = I18n.get(countKey, count);
			}
			return string.replace(I18n.get(I18n.get("menu.modded")), newString);
		}
		return string;
	}
}
