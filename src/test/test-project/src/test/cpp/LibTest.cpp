
#include "LibTest.hpp"
#include "../../main/cpp/Lib.hpp"

#include <sstream>

using Balau::Testing::is;

void LibTest::test() {
	std::ostringstream stream;

	helloTest(stream);

	const std::string expected = stream.str();

    AssertThat(expected, is("Hello, Test world.\n"));
}
