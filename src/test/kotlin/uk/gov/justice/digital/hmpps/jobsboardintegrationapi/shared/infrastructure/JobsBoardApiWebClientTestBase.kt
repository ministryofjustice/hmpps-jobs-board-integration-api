package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure

import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.application.UnitTestBase

abstract class JobsBoardApiWebClientTestBase : UnitTestBase() {
  @Mock
  protected lateinit var webClient: WebClient

  @InjectMocks
  protected lateinit var jobsBoardApiWebClient: JobsBoardApiWebClient

  val requestUriMock = mock(WebClient.RequestHeadersUriSpec::class.java)
  val requestHeadersMock = mock(WebClient.RequestHeadersSpec::class.java)
  val responseSpecMock = mock(WebClient.ResponseSpec::class.java)

  private fun onRequest() {
    whenever(webClient.get()).thenReturn(requestUriMock)
    whenever(requestHeadersMock.accept(APPLICATION_JSON)).thenReturn(requestHeadersMock)
    whenever(requestHeadersMock.retrieve()).thenReturn(responseSpecMock)
  }

  private fun onRequestById(uri: String, id: String) {
    onRequest()
    whenever(requestUriMock.uri(uri, id)).thenReturn(requestHeadersMock)
  }

  protected fun <T> replyOnRequestById(elementClass: Class<T>, response: T & Any, uri: String, id: String) {
    onRequestById(uri, id)
    whenever(responseSpecMock.bodyToMono(elementClass)).thenReturn(Mono.just(response))
  }
}
