.global init

.eqv GPIO_ENABLE_REG, 0x60004024
.eqv GPIO_OUT_REG, 0x60004004

.eqv IO_MUX_GPIO4_REG, 0x60009014
.eqv IO_MUX_GPIO5_REG, 0x60009018
.eqv IO_MUX_GPIO6_REG, 0x6000901C
.eqv IO_MUX_GPIO7_REG, 0x60009020

.text

init:
    li a1, GPIO_ENABLE_REG
    li a2, 0xFF
    sw a2, 0(a1)

    li a2, 0x1000

    li a1, IO_MUX_GPIO4_REG
    sw a2, 0(a1)

    li a1, IO_MUX_GPIO5_REG
    sw a2, 0(a1)

    li a1, IO_MUX_GPIO6_REG
    sw a2, 0(a1)

    li a1, IO_MUX_GPIO7_REG
    sw a2, 0(a1)

    li a1, GPIO_OUT_REG
    li a2, 0x80      # số 8
    sw a2, 0(a1)

loop:
    j loop