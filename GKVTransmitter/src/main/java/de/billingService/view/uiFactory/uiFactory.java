package de.billingService.view.uiFactory;

import lombok.Getter;

@Getter
public class uiFactory {

    private static uiFactory instanceFactory;

    public uiFactory() {
    }

    public static uiFactory getInstance() {
        if (instanceFactory == null) {
            instanceFactory = new uiFactory();
        }
        return instanceFactory;
    }
}
