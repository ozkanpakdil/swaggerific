package com.mascix.swaggerific;

import javafx.stage.Window;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class LoaderAnimationAspect {

    private final Window owner; // The owner window for the loading animation

    public LoaderAnimationAspect(Window owner) {
        this.owner = owner;
    }

    @Pointcut("@annotation(DisableWindow)")
    public void loaderAnimationPointcut() {
        System.out.println("pointcut");
    }

    @Before("loaderAnimationPointcut()")
    public void enableLoadingAnimation() {
        System.out.println("window should be disabled and show loader");
        Loader.enableLoadingAnimation(owner);
    }

    @After("loaderAnimationPointcut()")
    public void disableLoadingAnimation() {
        System.out.println("clear loader");
        Loader.disableLoadingAnimation();
    }
}
