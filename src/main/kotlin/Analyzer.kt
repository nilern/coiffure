class Analyzer {
    companion object {
        fun analyze(form: Any?): Expr {
            if (form is Long) {
                return Const.create(form)
            } else {
                TODO()
            }
        }
    }
}
