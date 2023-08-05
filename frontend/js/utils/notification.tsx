function sendNotification(head: string, body: string) {
    const notification = new Notification(head, {
        //icon: "",
        body: body
    })
}

export function runNotification(head: string, body: string) {
    if (!("Notification" in window)) {
        alert("This browser does not support system notifications!")
    } else if (Notification.permission === "granted") {
        sendNotification(head, body)
    } else if (Notification.permission !== "denied") {
        Notification.requestPermission((permission) => {
            if (permission === "granted") {
                sendNotification(head, body)
            }
        })
    } else {
        //alert("Пожалуйста, разрешите оповещения :(")
    }
}