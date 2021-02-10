package codes.biscuit.skyblockaddons.asm.hooks;

import codes.biscuit.skyblockaddons.SkyblockAddons;
import codes.biscuit.skyblockaddons.core.Feature;
import codes.biscuit.skyblockaddons.core.InventoryType;
import codes.biscuit.skyblockaddons.features.backpacks.ContainerPreviewManager;
import codes.biscuit.skyblockaddons.features.backpacks.ContainerPreview;
import codes.biscuit.skyblockaddons.features.cooldowns.CooldownManager;
import codes.biscuit.skyblockaddons.utils.InventoryUtils;
import codes.biscuit.skyblockaddons.utils.ItemUtils;
import codes.biscuit.skyblockaddons.utils.skyblockdata.ContainerItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import org.lwjgl.input.Keyboard;

public class GuiScreenHook {

    private static final int MADDOX_BATPHONE_COOLDOWN = 20 * 1000;

    //TODO Fix for Hypixel localization
    public static boolean onRenderTooltip(ItemStack itemStack, int x, int y) {
        boolean cancelled = false;

        SkyblockAddons main = SkyblockAddons.getInstance();
        if (main.getConfigValues().isEnabled(Feature.DISABLE_EMPTY_GLASS_PANES) && main.getUtils().isEmptyGlassPane(itemStack)) {
            return true;
        }

        // Cancel tooltips while containers are frozen and we aren't trying to render a tooltip in the backpack
        if (ContainerPreviewManager.isFrozen() && !ContainerPreviewManager.isRenderingBackpackTooltip()) {
            return true;
        }

        if (main.getConfigValues().isDisabled(Feature.SHOW_EXPERIMENTATION_TABLE_TOOLTIPS) &&
                (main.getInventoryUtils().getInventoryType() == InventoryType.ULTRASEQUENCER ||
                main.getInventoryUtils().getInventoryType() == InventoryType.CHRONOMATRON)) {
            return true;
        }

        if (main.getConfigValues().isEnabled(Feature.SHOW_BACKPACK_PREVIEW)) {
            // Don't show if we only want to show while holding shift, and the player isn't holding shift
            if (main.getConfigValues().isEnabled(Feature.SHOW_BACKPACK_HOLDING_SHIFT) && !GuiScreen.isShiftKeyDown()) {
                return false;
            }
            // Don't render the preview the item represents a crafting recipe or the result of one.
            if (ItemUtils.isMenuItem(itemStack)) {
                return false;
            }
            // Check the subfeature conditions
            NBTTagCompound extraAttributes = ItemUtils.getExtraAttributes(itemStack);
            ContainerItem containerItem = ItemUtils.itemMap.getContainerItem(ItemUtils.getSkyBlockItemID(extraAttributes));
            // TODO: Does checking menu item handle the baker inventory thing?
            if (containerItem == null ||
                    (containerItem.isCakeBag() && (main.getConfigValues().isDisabled(Feature.CAKE_BAG_PREVIEW))) /*|| main.getInventoryUtils().getInventoryType() != InventoryType.BAKER) */||
                    (containerItem.isPersonalCompactor() && main.getConfigValues().isDisabled(Feature.SHOW_PERSONAL_COMPACTOR_PREVIEW))) {
                return false;
            }

            //TODO: Probably some optimizations here we can do. Can we check chest equivalence?
            // Avoid showing backpack preview in auction stuff.
            Container playerContainer = Minecraft.getMinecraft().thePlayer.openContainer;
            if (playerContainer instanceof ContainerChest) {
                IInventory chestInventory = ((ContainerChest) playerContainer).getLowerChestInventory();
                if (chestInventory.hasCustomName()) {
                    String chestName = chestInventory.getDisplayName().getUnformattedText();
                    if (chestName.contains("Auction") || "Your Bids".equals(chestName)) {

                        // Make sure this backpack is in the auction house and not just in your inventory before cancelling.
                        for (int slotNumber = 0; slotNumber < chestInventory.getSizeInventory(); slotNumber++) {
                            if (chestInventory.getStackInSlot(slotNumber) == itemStack) {
                                return false;
                            }
                        }
                    }
                }
            }

            ContainerPreview containerPreview = ContainerPreviewManager.getFromItem(itemStack);
            if (containerPreview != null) {
                containerPreview.setX(x);
                containerPreview.setY(y);

                // Handle the freeze container toggle

                if (isFreezeKeyDown() && System.currentTimeMillis() - ContainerPreviewManager.getLastToggleFreezeTime() > 500) {
                    ContainerPreviewManager.setLastToggleFreezeTime(System.currentTimeMillis());
                    ContainerPreviewManager.setFrozen(!ContainerPreviewManager.isFrozen());
                    ContainerPreviewManager.setContainerPreview(containerPreview);
                }

                if (!ContainerPreviewManager.isFrozen()) {
                    ContainerPreviewManager.setContainerPreview(containerPreview);
                }
                main.getPlayerListener().onItemTooltip(new ItemTooltipEvent(itemStack, null, null, false));
                cancelled = true;
            }
        }

        if (ContainerPreviewManager.isFrozen()) {
            cancelled = true;
        }

        return cancelled;
    }

    /**
     * Returns whether the backpack freeze key is down
     *
     * @return {@code true} if the backpack freeze key is down, {@code false} otherwise
     */
    private static boolean isFreezeKeyDown() {
        SkyblockAddons main = SkyblockAddons.getInstance();
        if (main.getFreezeBackpackKey().isKeyDown()) return true;
        try {
            if (Keyboard.isKeyDown(main.getFreezeBackpackKey().getKeyCode())) return true;
        } catch (Exception ignored) {}

        return false;
    }

    //TODO: Fix for Hypixel localization
    public static void handleComponentClick(IChatComponent component) {
        SkyblockAddons main = SkyblockAddons.getInstance();
        if (main.getUtils().isOnSkyblock() && component != null && "§2§l[OPEN MENU]".equals(component.getUnformattedText()) &&
                !CooldownManager.isOnCooldown(InventoryUtils.MADDOX_BATPHONE_DISPLAYNAME)) {// The prompt when Maddox picks up the phone.
            CooldownManager.put(InventoryUtils.MADDOX_BATPHONE_DISPLAYNAME, MADDOX_BATPHONE_COOLDOWN);
        }
    }
}
