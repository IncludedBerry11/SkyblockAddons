package codes.biscuit.skyblockaddons.utils.data.requests;

import codes.biscuit.skyblockaddons.SkyblockAddons;
import codes.biscuit.skyblockaddons.misc.scheduler.SkyblockRunnable;
import codes.biscuit.skyblockaddons.utils.data.DataUtils;
import codes.biscuit.skyblockaddons.utils.data.JSONResponseHandler;
import codes.biscuit.skyblockaddons.utils.data.RemoteFileRequest;
import com.google.gson.JsonObject;
import net.minecraft.util.EnumChatFormatting;

import java.util.concurrent.ExecutionException;

public class MayorRequest extends RemoteFileRequest<JsonObject> {
    public MayorRequest() {
            super("https://api.hypixel.net/resources/skyblock/election"
                    , new JSONResponseHandler<>(JsonObject.class)
                    , false
                    , true);
        }

    @Override
    public void load() throws InterruptedException, ExecutionException, RuntimeException {
        SkyblockAddons main = SkyblockAddons.getInstance();
        String mayorName = getResult().get("mayor").getAsJsonObject().get("name").getAsString();

        main.getUtils().setMayor(mayorName == null ? "Fix3dll" : mayorName);

        // Jerry's Perkpocalypse mayor updater
        main.getNewScheduler().runAsync(new SkyblockRunnable() {
            @Override
            public void run() {
                if (main.getUtils().getMayor().startsWith("Jerry") && System.currentTimeMillis() > main.getUtils().getJerryMayorUpdateTime()) {
                    DataUtils.loadOnlineData(new JerryMayorRequest());

                    String name = main.getUtils().getJerryMayor();
                    SkyblockAddons.getLogger().info("Jerry's Perkpocalypse mayor switched to " + name);
                    if (main.getUtils().isOnSkyblock())
                        main.getUtils().sendMessage(EnumChatFormatting.GREEN + "Jerry's Perkpocalypse mayor switched to " + name, true);
                }
            }
        }, 0, 60 * 20);
    }
}
