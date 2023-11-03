/**
 * Отображает оповещение с указанным заголовком и контентом.
 */
export function runNotification(head: string, body: string) {
    const showNotification = () => {
        new Notification(head, {
            body: body
        });
    };

    if (!("Notification" in window)) {
        alert("This browser does not support system notifications!")
    } else if (Notification.permission === "granted") {
        showNotification();
    } else if (Notification.permission !== "denied") {
        Notification.requestPermission((permission) => {
            if (permission === "granted") {
                showNotification();
            }
        })
    } else {
        //alert("Пожалуйста, разрешите оповещения :(")
    }
}