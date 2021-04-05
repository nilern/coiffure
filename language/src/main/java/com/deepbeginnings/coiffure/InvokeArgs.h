#define PARAM(param) Object param
#define PARAMS0()
#define PARAMS1(param) PARAM(param)
#define PARAMS2(param, ...) PARAM(param), PARAMS1(__VA_ARGS__)
#define PARAMS3(param, ...) PARAM(param), PARAMS2(__VA_ARGS__)
#define PARAMS4(param, ...) PARAM(param), PARAMS3(__VA_ARGS__)
#define PARAMS5(param, ...) PARAM(param), PARAMS4(__VA_ARGS__)

#define ARG(arg) Util.ret1(arg, arg = null)
#define ARGS0()
#define ARGS1(arg) , ARG(arg)
#define ARGS2(arg, ...) , ARG(arg) ARGS1(__VA_ARGS__)
#define ARGS3(arg, ...) , ARG(arg) ARGS2(__VA_ARGS__)
#define ARGS4(arg, ...) , ARG(arg) ARGS3(__VA_ARGS__)
#define ARGS5(arg, ...) , ARG(arg) ARGS4(__VA_ARGS__)
