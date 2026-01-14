package model;

import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.plugin.SimplePlugin;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ItemRenderQueue {

    private static final ItemRenderQueue instance = new ItemRenderQueue();

    // Очередь задач. Task = Генерация предмета + Действие после (обновление меню)
    private final Queue<RenderTask> queue = new ConcurrentLinkedQueue<>();
    private boolean isRunning = false;

    // СКОРОСТЬ: 5 предметов за 1 тик (100 в секунду). Настрой под свой сервер.
    private static final int ITEMS_PER_TICK = 5;

    public static ItemRenderQueue getInstance() { return instance; }

    /**
     * Добавить задачу на генерацию предмета.
     * @param itemGenerator Тяжелая логика создания ItemStack
     * @param onComplete Что сделать, когда предмет готов (обычно - обновить меню)
     */
    public void queue(Supplier<ItemStack> itemGenerator, Consumer<ItemStack> onComplete) {
        queue.add(new RenderTask(itemGenerator, onComplete));
        startProcessor();
    }

    private void startProcessor() {
        if (isRunning) return;
        isRunning = true;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (queue.isEmpty()) {
                    isRunning = false;
                    this.cancel();
                    return;
                }

                // Обрабатываем пачку
                for (int i = 0; i < ITEMS_PER_TICK; i++) {
                    RenderTask task = queue.poll();
                    if (task == null) break;

                    // Запускаем генерацию В АСИНХРОНЕ
                    Common.runAsync(() -> {
                        try {
                            ItemStack result = task.generator.get();

                            // Возвращаем результат В ОСНОВНОЙ ПОТОК
                            Common.runLater(() -> task.callback.accept(result));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
            }
        }.runTaskTimer(SimplePlugin.getInstance(), 1, 1);
    }

    private static class RenderTask {
        final Supplier<ItemStack> generator;
        final Consumer<ItemStack> callback;

        public RenderTask(Supplier<ItemStack> generator, Consumer<ItemStack> callback) {
            this.generator = generator;
            this.callback = callback;
        }
    }
}
