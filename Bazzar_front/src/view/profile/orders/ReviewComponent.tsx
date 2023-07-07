import {
    Button, Checkbox,
    Dialog,
    DialogActions,
    DialogContent,
    DialogContentText,
    DialogTitle,
    Rating,
    TextField,
    Typography
} from "@mui/material";
import {useKeycloak} from "@react-keycloak/web";
import React, {useState} from "react";
import {primary} from "../../../Colors";
import {apiAddReview} from "../../../api/ReviewApi";
import {useError} from "../../../auth/ErrorProvider";
import {OrderItem, ReviewDto} from "../../../newInterfaces";

interface ReviewProps {
    product: OrderItem
}

export function ReviewComponent({product}: ReviewProps) {
    const [open, setOpen] = useState(false);
    const [value, setValue] = useState<number>(0);
    const [text, setText] = useState<string>("");
    const {keycloak, initialized} = useKeycloak();
    const [email, setEmail] = React.useState<string>(keycloak?.tokenParsed?.email ?? "");
    const error = useError();
    const [checked, setChecked] = React.useState(false);

    const handleClickOpen = () => {
        setOpen(true);
    };

    const handleClose = () => {
        setOpen(false);
    };


    const payHandle = () => {
        if (value > 0) {
            if (text.length > 5 && email) {
                const data: ReviewDto = {
                    mark: value,
                    fullName: checked ? keycloak.tokenParsed?.name.substring(0,3).concat("...") : keycloak.tokenParsed?.name,
                    reviewText: text,
                    username: email,
                    productId: product.productId
                };
                apiAddReview(data).then(() => {
                    setOpen(false);
                    error.setErrors("", true, true, "Отзыв оставлен");
                    error.setShow(true)
                }).catch(() => {
                    setOpen(false);
                    error.setErrors("Ошибка при добавлении отзыва", false, false, "");
                    error.setShow(true)
                })
            }
        }
    };

    const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        setChecked(event.target.checked);
    };

    return (
        <div>
            <button className="btn btn-sm text-white" style={{backgroundColor: primary}}
                    onClick={handleClickOpen}>Оставить отзыв
            </button>
            <Dialog open={open} onClose={handleClose}>
                <DialogTitle>Оставить отзыв</DialogTitle>
                <DialogContent>
                    <DialogContentText>
                        Здесь вы можете оставить отзыв о товаре
                    </DialogContentText>
                    <br/>
                    <Typography component="legend">Поставьте оценку от 1 до 5</Typography>
                    <Rating
                        name="simple-controlled"
                        value={value}
                        onChange={(event, newValue) => {
                            setValue(newValue != null ? newValue : 0);
                        }}
                    />
                    <br/>
                    <div className="d-flex justify-content-start align-items-center">
                        <Typography component="legend">Оставить отзыв анонимно</Typography>
                        <Checkbox
                            checked={checked}
                            onChange={handleChange}
                            inputProps={{'aria-label': 'controlled'}}
                        />
                    </div>

                    <TextField
                        margin="dense"
                        id="name"
                        label="Отзыв"
                        type="text"
                        onChange={(event) => setText(event.currentTarget.value)}
                        fullWidth
                        multiline={true}
                        variant="standard"
                    />
                </DialogContent>
                <DialogActions>
                    <Button onClick={payHandle}>Отправить</Button>
                </DialogActions>
            </Dialog>
        </div>
    )
}