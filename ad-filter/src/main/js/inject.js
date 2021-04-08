(function () {
    const onReadystatechange = function () {
        if (document.readyState == 'interactive') {
            script();
        }
    }
    const addListener = function () {
        // here don't use document.onreadystatechange, which won't fire sometimes
        document.addEventListener('readystatechange', onReadystatechange);

        document.addEventListener('DOMContentLoaded', script, false);

        window.addEventListener('load', script);
    }
    const removeListener = function () {
        document.removeEventListener('readystatechange', onReadystatechange);

        document.removeEventListener('DOMContentLoaded', script, false);

        window.removeEventListener('load', script);
    }
    const script = function () {
        {{INJECTION}}
        removeListener();
    }
    if (document.readyState == 'interactive' || document.readyState == 'complete') {
        script();
    } else {
        addListener();
    }
})();