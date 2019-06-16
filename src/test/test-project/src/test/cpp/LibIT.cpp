
#include "LibIT.hpp"
#include "../../main/cpp/Lib.hpp"

#include <sstream>

using Balau::Testing::is;

void LibIT::test() {
	std::ostringstream stream;

	helloIT(stream);

	const std::string expected = stream.str();

    AssertThat(expected, is("Hello, IT world.\n"));
}
