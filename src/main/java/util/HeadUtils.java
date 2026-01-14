package util;

import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.SkinsRestorerProvider;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.storage.PlayerStorage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.menu.model.SkullCreator;

import java.util.Optional;
import java.util.UUID;

public class HeadUtils {

    // Метод для получения головы с учетом SkinRestorer
    public static ItemStack getPlayerHead(UUID uuid, String name) {
        try {
            // 1. Получаем API SkinRestorer
            SkinsRestorer skinsRestorerAPI = SkinsRestorerProvider.get();

            // 2. Получаем хранилище данных игроков
            PlayerStorage playerStorage = skinsRestorerAPI.getPlayerStorage();

            // 3. Пытаемся получить текущий скин игрока (по UUID и имени)
            Optional<SkinProperty> skin = playerStorage.getSkinForPlayer(uuid, name);

            // 4. Если скин найден в SkinRestorer
            if (skin.isPresent()) {
                // Получаем Base64 значение текстуры (Value)
                String base64Texture = skin.get().getValue();

                // 5. Создаем голову через Foundation, используя Base64
                return SkullCreator.itemFromBase64(base64Texture);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Фоллбэк: Если SkinRestorer не ответил или скина нет,
        // берем голову по UUID (будет реальный скин Mojang)
        return SkullCreator.itemFromUuid(uuid);
    }
}
