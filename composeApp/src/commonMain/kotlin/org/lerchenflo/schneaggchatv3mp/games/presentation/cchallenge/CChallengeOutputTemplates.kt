package org.lerchenflo.schneaggchatv3mp.games.presentation.cchallenge

import org.lerchenflo.schneaggchatv3mp.games.domain.SplitMix64

/**
 * A "what does this print" puzzle: the body of main(), what it prints and plausible wrong answers
 * (typical mistakes like off-by-one or forgetting a fall-through). Wrong answers that happen to
 * equal the output are dropped by the generator.
 */
internal class COutputPuzzle(
    val body: List<String>,
    val output: String,
    val wrong: List<String>,
)

/**
 * Each template picks random parameters and computes the output with the same algorithm in
 * Kotlin, so no C interpreter is needed. Keep every program free of undefined behaviour and
 * of anything platform dependent (sizeof, overflow, float rounding of non-exact values).
 */
internal val C_OUTPUT_TEMPLATES: List<(SplitMix64) -> COutputPuzzle> = listOf(
    // Exclusive loop bound
    { rng ->
        val from = rng.nextInt(0, 5)
        val until = rng.nextInt(from + 3, from + 9)
        COutputPuzzle(
            body = listOf(
                "int sum = 0;",
                "for (int i = $from; i < $until; i++) {",
                "    sum += i;",
                "}",
                "printf(\"%d\\n\", sum);",
            ),
            output = (from until until).sum().toString(),
            wrong = listOf((from..until).sum(), (from + 1 until until).sum(), until - from).map { it.toString() },
        )
    },
    // Prime factorization
    { rng ->
        val primes = listOf(2, 3, 5, 7, 11)
        val factors = List(rng.nextInt(2, 5)) { primes[rng.nextInt(primes.size)] }.sorted()
        val n = factors.fold(1) { acc, p -> acc * p }
        COutputPuzzle(
            body = listOf(
                "int n = $n;",
                "for (int p = 2; p * p <= n; p++) {",
                "    while (n % p == 0) {",
                "        printf(\"%d \", p);",
                "        n /= p;",
                "    }",
                "}",
                "if (n > 1) printf(\"%d\", n);",
                "printf(\"\\n\");",
            ),
            output = factors.joinToString(" "),
            wrong = listOf(
                factors.distinct().joinToString(" "),
                factors.dropLast(1).joinToString(" "),
                (listOf(1) + factors).joinToString(" "),
                (factors + n).joinToString(" "),
            ),
        )
    },
    // Integer division and modulo
    { rng ->
        val b = rng.nextInt(3, 10)
        val a = b * rng.nextInt(2, 9) + rng.nextInt(1, b)
        val q = a / b
        val r = a % b
        COutputPuzzle(
            body = listOf(
                "int a = $a, b = $b;",
                "printf(\"%d %d\\n\", a / b, a % b);",
            ),
            output = "$q $r",
            wrong = listOf("$r $q", "${q + 1} $r", "$q ${b - r}", "$q 0"),
        )
    },
    // Post- and pre-increment
    { rng ->
        val x = rng.nextInt(2, 20)
        COutputPuzzle(
            body = listOf(
                "int x = $x;",
                "int y = x++;",
                "int z = ++x;",
                "printf(\"%d %d %d\\n\", x, y, z);",
            ),
            output = "${x + 2} $x ${x + 2}",
            wrong = listOf("${x + 2} ${x + 1} ${x + 2}", "${x + 1} $x ${x + 1}", "${x + 2} ${x + 1} ${x + 3}"),
        )
    },
    // Iterative Fibonacci
    { rng ->
        val n = rng.nextInt(5, 13)
        fun fib(k: Int): Int {
            var a = 0
            var b = 1
            repeat(k) { val next = a + b; a = b; b = next }
            return a
        }
        COutputPuzzle(
            body = listOf(
                "int a = 0, b = 1;",
                "for (int i = 0; i < $n; i++) {",
                "    int next = a + b;",
                "    a = b;",
                "    b = next;",
                "}",
                "printf(\"%d\\n\", a);",
            ),
            output = fib(n).toString(),
            wrong = listOf(fib(n - 1), fib(n + 1), fib(n + 2)).map { it.toString() },
        )
    },
    // Reversing the digits of a number
    { rng ->
        val n = rng.nextInt(100, 10000)
        val reversedText = n.toString().reversed()
        COutputPuzzle(
            body = listOf(
                "int n = $n, rev = 0;",
                "while (n > 0) {",
                "    rev = rev * 10 + n % 10;",
                "    n /= 10;",
                "}",
                "printf(\"%d\\n\", rev);",
            ),
            output = reversedText.toInt().toString(),
            wrong = listOf(reversedText, n.toString(), n.toString().sumOf { it - '0' }.toString(), (reversedText.toInt() * 10).toString()),
        )
    },
    // Euclid's algorithm
    { rng ->
        val g = rng.nextInt(2, 10)
        val m = rng.nextInt(2, 10)
        var k = rng.nextInt(2, 10)
        if (k == m) k++
        val a = g * m
        val b = g * k
        fun gcd(x: Int, y: Int): Int = if (y == 0) x else gcd(y, x % y)
        val result = gcd(a, b)
        COutputPuzzle(
            body = listOf(
                "int a = $a, b = $b;",
                "while (b != 0) {",
                "    int r = a % b;",
                "    a = b;",
                "    b = r;",
                "}",
                "printf(\"%d\\n\", a);",
            ),
            output = result.toString(),
            wrong = listOf(a * b / result, minOf(a, b), 0, g, 1).map { it.toString() },
        )
    },
    // Nested loops with a dependent start
    { rng ->
        val outer = rng.nextInt(2, 6)
        val inner = rng.nextInt(outer, outer + 4)
        var count = 0
        for (i in 0 until outer) for (j in i until inner) count++
        COutputPuzzle(
            body = listOf(
                "int count = 0;",
                "for (int i = 0; i < $outer; i++) {",
                "    for (int j = i; j < $inner; j++) {",
                "        count++;",
                "    }",
                "}",
                "printf(\"%d\\n\", count);",
            ),
            output = count.toString(),
            wrong = listOf(outer * inner, count + outer, count - 1, (outer + 1) * inner).map { it.toString() },
        )
    },
    // Bit shift
    { rng ->
        val a = rng.nextInt(1, 10)
        val s = rng.nextInt(1, 5)
        COutputPuzzle(
            body = listOf(
                "int a = $a;",
                "printf(\"%d\\n\", a << $s);",
            ),
            output = (a shl s).toString(),
            wrong = listOf(a * s, a shl (s + 1), a shr 1, a + s).map { it.toString() },
        )
    },
    // Bitwise AND
    { rng ->
        val a = rng.nextInt(5, 32)
        val b = rng.nextInt(5, 32)
        COutputPuzzle(
            body = listOf(
                "int a = $a, b = $b;",
                "printf(\"%d\\n\", a & b);",
            ),
            output = (a and b).toString(),
            wrong = listOf(a or b, a xor b, a + b, 1).map { it.toString() },
        )
    },
    // Character arithmetic
    { rng ->
        val c = 'a' + rng.nextInt(1, 21)
        COutputPuzzle(
            body = listOf(
                "char c = 'a' + ${c - 'a'};",
                "printf(\"%c%c\\n\", c, c - 32);",
            ),
            output = "$c${c.uppercaseChar()}",
            wrong = listOf(
                "${c + 1}${(c + 1).uppercaseChar()}",
                "${c - 1}${(c - 1).uppercaseChar()}",
                "${c.uppercaseChar()}$c",
                "$c$c",
            ),
        )
    },
    // Integer division before the conversion to double
    { rng ->
        val b = if (rng.nextInt(2) == 0) 2 else 5
        val a = b * rng.nextInt(1, 9) + rng.nextInt(1, b)
        val tenths = a * 10 / b
        COutputPuzzle(
            body = listOf(
                "double d = $a / $b;",
                "printf(\"%.1f\\n\", d);",
            ),
            output = "${a / b}.0",
            wrong = listOf("${tenths / 10}.${tenths % 10}", "${a / b + 1}.0", "${a / b}", "${a % b}.0"),
        )
    },
    // switch fall-through
    { rng ->
        val x = rng.nextInt(1, 4)
        val output = when (x) {
            1 -> 11
            2 -> 10
            else -> 1100
        }
        COutputPuzzle(
            body = listOf(
                "int x = $x, r = 0;",
                "switch (x) {",
                "    case 1: r += 1;",
                "    case 2: r += 10;",
                "        break;",
                "    case 3: r += 100;",
                "    default: r += 1000;",
                "}",
                "printf(\"%d\\n\", r);",
            ),
            output = output.toString(),
            wrong = listOf(1, 10, 11, 100, 1100, 1111, 1000).map { it.toString() },
        )
    },
    // continue and break in one loop
    { rng ->
        val n = rng.nextInt(6, 16)
        val limit = rng.nextInt(3, n)
        var sum = 0
        for (i in 1..n) {
            if (i % 2 == 0) continue
            if (i > limit) break
            sum += i
        }
        COutputPuzzle(
            body = listOf(
                "int sum = 0;",
                "for (int i = 1; i <= $n; i++) {",
                "    if (i % 2 == 0) continue;",
                "    if (i > $limit) break;",
                "    sum += i;",
                "}",
                "printf(\"%d\\n\", sum);",
            ),
            output = sum.toString(),
            wrong = listOf(
                (1..n).filter { it % 2 == 1 }.sum(),
                (1..limit).sum(),
                (1..limit).filter { it % 2 == 0 }.sum(),
                sum + limit + 1,
            ).map { it.toString() },
        )
    },
)
