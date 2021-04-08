(function () {
    {{DEBUG}} console.log('element hiding started');

    // hide by injecting CSS stylesheet
    {{DEBUG}} console.log('starting injecting eh css rules for ' + document.location.href);
    var styleSheet = {{BRIDGE}}.getEleHidingStyleSheet(document.location.href);
    if (styleSheet) {
        {{DEBUG}} console.log('stylesheet length: ' + styleSheet.length);
        var head = document.getElementsByTagName('head')[0];
        var style = document.createElement('style');
        head.appendChild(style);
        style.textContent = styleSheet;
        {{DEBUG}} console.log('finished injecting css rules');
    } else {
        {{DEBUG}} console.log('stylesheet is empty, skipping injection');
    }

    // hide by ExtendedCss
    try {
        var css = {{BRIDGE}}.getExtendedCssStyleSheet(document.location.href);
        {{DEBUG}} console.log(`ExtendedCss rules(length: ${css.length}) injecting for ${document.location.href}`);
        if (css.length > 0) {
            var extendedCss = new ExtendedCss({ styleSheet: css });
            extendedCss.apply();
        }
        {{DEBUG}} console.log(`ExtendedCss rules success for ${document.location.href}`);
    } catch (err) {
        {{DEBUG}} console.log(`ExtendedCss rules failed '${css}' for ${document.location.href} by ${err}`);
        throw err;
    }
})();