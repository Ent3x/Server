package org.cloudburstmc.server.enchantment.behavior;

public class EnchantmentBindingCurse extends EnchantmentBehavior {

    @Override
    public int getMinEnchantAbility(int level) {
        return 25;
    }

    @Override
    public int getMaxEnchantAbility(int level) {
        return 50;
    }

}
