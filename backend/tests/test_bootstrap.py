import asyncio
import unittest

from tests.asgi_client import request_app


class BootstrapEndpointTest(unittest.TestCase):
    def test_get_bootstrap_requires_authentication(self) -> None:
        status, body = asyncio.run(request_app("GET", "/me/bootstrap"))

        self.assertEqual(status, 401)
        self.assertEqual(body["detail"], "Not authenticated.")


if __name__ == "__main__":
    unittest.main()
