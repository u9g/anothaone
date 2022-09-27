package net.fabricmc.example;

public class PrisonsIntegration {
    public static boolean isOnPrisons = true;

    public static boolean isVaultScreen(String containerName) {
        return containerName.trim().startsWith("Vault ");
    }
}
