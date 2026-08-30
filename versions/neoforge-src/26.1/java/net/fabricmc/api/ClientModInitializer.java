package net.fabricmc.api;

/** Compile-time bridge that lets the shared 26.1 client source keep its Fabric entry class. */
public interface ClientModInitializer {
    void onInitializeClient();
}
