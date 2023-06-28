import {Button, Dialog, DialogActions, DialogTitle} from "@mui/material";
import React, {useState} from "react";
import {apiOrderRefund} from "../../../api/OrderApi";
import {Order} from "../../../newInterfaces";

interface PayFormProps {
    order: Order
    onReloadOrder: () => void
    setStatus: (status: boolean) => void
}

export function DeleteOrderForm(props: PayFormProps) {
    const [open, setOpen] = useState(false);


    const handleClickOpen = () => {
        setOpen(true);
    };

    const handleClose = () => {
        setOpen(false);
    };

    const deleteHandle = () => {
        apiOrderRefund(props.order.id).then(() => {
                handleClose();
                props.onReloadOrder();
        })
    };

    return (
        <div>
            <button className="btn btn-sm btn-danger" onClick={handleClickOpen}>Отмена</button>
            <Dialog open={open} onClose={handleClose}>
                <DialogTitle>Отмена заказа</DialogTitle>
                <DialogActions>
                    <Button onClick={deleteHandle}>Отменить</Button>
                </DialogActions>
            </Dialog>
        </div>
    )
}