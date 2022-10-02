package dev.u9g.neustoragegui;

public class PrisonsIntegration {
    public static boolean isOnPrisons = true;

    public static boolean isVaultScreen(String containerName) {
        return containerName.trim().startsWith("§lVault ");
    }

    public static int getVaultPageFromName(String containerName) {
        if (!isVaultScreen(containerName)) throw new Error("Not vault name...");
        return Integer.parseInt(containerName.trim().replace("§lVault ", "").split(" ")[0]);
    }

    public static boolean isGeneralVaultsScreen(String containerName) {
        return containerName.trim().startsWith("§lVaults ");
    }
}
